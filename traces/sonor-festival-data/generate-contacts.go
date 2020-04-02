package main

import (
	"encoding/csv"
	"fmt"
	"os"
	"strconv"
	"time"
)

const csvFile string = "no-dups-anonymized-sonar-data.csv"
const newCsvFile string = "contacts-parsed-sonar-data.csv"
const macAddr int = 0
const loc int = 1
const timestamp int = 2
const day1 int = 18
const day2 int = 19
const day3 int = 20

var result [][]string

type EventRecord struct {
	nodeId int
	start  int64
	end    int64
}

var day1Map map[string][]EventRecord
var day2Map map[string][]EventRecord
var day3Map map[string][]EventRecord

func min(a, b int64) int64 {
	if a < b {
		return a
	}
	return b
}

func createContacts(myMap map[string][]EventRecord, writer *csv.Writer) {
	var contacts int64

	for key, mySlice := range myMap {
		// fmt.Println("mySlice size " + strconv.Itoa(len(mySlice)) + " location " + key)
		for i := 0; i < len(mySlice)-1; i++ {
			contacts = 0
			nodeId := mySlice[i].nodeId
			for j := i + 1; j < len(mySlice); j++ {
				// this condition is necessary because a node might return
				// multiple times during a day at a certain location
				if nodeId == mySlice[j].nodeId {
					continue
				}
				// if node A arrives at the location X
				// and node B was already there, create a
				// contact opportunity
				if mySlice[i].start > mySlice[j].start {
					err := writer.Write([]string{
						strconv.Itoa(nodeId),
						strconv.Itoa(mySlice[j].nodeId),
						strconv.FormatInt(mySlice[i].start, 10),
						strconv.FormatInt(min(mySlice[i].end, mySlice[j].end), 10),
						key})
					if err != nil {
						panic(err)
					}
					contacts++
				}
			}
		}
		// fmt.Println("contacts at this location " + strconv.FormatInt(contacts, 10))
	}
}

func main() {
	day1Map = make(map[string][]EventRecord)
	day2Map = make(map[string][]EventRecord)
	day3Map = make(map[string][]EventRecord)

	file, err := os.OpenFile(csvFile, os.O_RDWR, 0660)
	if err != nil {
		panic(err)
	}

	reader := csv.NewReader(file)
	// read all records
	result, _ = reader.ReadAll()

	nodeID := 0
	strNodeID := strconv.Itoa(nodeID)
	currentMac := result[1][macAddr]
	result[1][macAddr] = strNodeID
	// convert the timestamp to unix time
	layout := "2006-01-02 15:04:05"
	t, err := time.Parse(layout, result[1][timestamp])
	day := t.Day()
	fmt.Println(day)
	location := result[1][loc]
	startTime, prevTime := t.Unix(), t.Unix()
	fmt.Println(startTime)

	for i := 2; i < len(result); i++ {
		// convert the timestamp to unix time
		t, err = time.Parse(layout, result[i][timestamp])
		// set the nodeID
		if result[i][macAddr] == currentMac {
			result[i][macAddr] = strNodeID
			/*
			 * if the day and the location have not changed,
			 * contiune to look for the moment the node left that
			 * location
			 */
			// fmt.Printf("node %d loc1 %s loc2 %s day1 %d day2 %d\n", nodeID, location, result[i][loc], day, t.Day())
			if result[i][loc] == location && t.Day() == day {
				prevTime = t.Unix()
				continue
			} else {
				/*
				 * the node was spotted only once at the scene
				 * and that information is useless, thus we
				 * prepare the  variables for the next day or
				 * location
				 */
				if prevTime == startTime {
					location = result[i][loc]
					day = t.Day()
					startTime, prevTime = t.Unix(), t.Unix()
					continue
				} else {
					event := EventRecord{
						nodeID,
						startTime,
						prevTime,
					}

					switch day {
					case day1:
						// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", nodeID, location, day, startTime, prevTime)
						day1Map[location] = append(day1Map[location], event)
						break
					case day2:
						// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", nodeID, location, day, startTime, prevTime)
						day2Map[location] = append(day2Map[location], event)
						break
					case day3:
						// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", nodeID, location, day, startTime, prevTime)
						day3Map[location] = append(day3Map[location], event)
						break
					default:
						fmt.Println("bad day format")
					}

					location = result[i][loc]
					day = t.Day()
					startTime, prevTime = t.Unix(), t.Unix()
				}
			}
		} else {
			currentMac = result[i][macAddr]
			nodeID++
			strNodeID = strconv.Itoa(nodeID)
			result[i][macAddr] = strNodeID

			if prevTime == startTime {
				location = result[i][loc]
				day = t.Day()
				startTime, prevTime = t.Unix(), t.Unix()
				continue
			} else {
				lastID := nodeID - 1
				event := EventRecord{
					lastID,
					startTime,
					prevTime,
				}
				switch day {
				case day1:
					// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", lastID, location, day, startTime, prevTime)
					day1Map[location] = append(day1Map[location], event)
					day1Map[location] = append(day1Map[location], event)
					break
				case day2:
					// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", lastID, location, day, startTime, prevTime)
					day1Map[location] = append(day1Map[location], event)
					day2Map[location] = append(day2Map[location], event)
					break
				case day3:
					// fmt.Printf("ADD node %d loc %s day %d startT %d endT %d\n", lastID, location, day, startTime, prevTime)
					day1Map[location] = append(day1Map[location], event)
					day3Map[location] = append(day3Map[location], event)
					break
				default:
					fmt.Println("bad day format")
				}

				// set the variables for the new node
				location = result[i][loc]
				day = t.Day()
				startTime, prevTime = t.Unix(), t.Unix()
			}
		}
	}

	file.Close()

	file, err = os.OpenFile(newCsvFile, os.O_CREATE|os.O_WRONLY, 0660)
	if err != nil {
		panic(err)
	}
	writer := csv.NewWriter(file)

	// for key, mySlice := range day1Map {
	// 	fmt.Println("mySlice size " + strconv.Itoa(len(mySlice)) + " location " + key)
	// 	for i := 0; i < len(mySlice); i++ {
	// 		fmt.Printf("%d %d %d %s\n", mySlice[i].nodeId, mySlice[i].start, mySlice[i].end, key)
	// 	}
	// }

	// establish contacts between nodes
	createContacts(day1Map, writer)
	// createContacts(day2Map, writer)
	// createContacts(day3Map, writer)
}

package mobemu.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;

import mobemu.parsers.Host;

public class FestivalMobilityComponent extends JComponent {

	private static final long serialVersionUID = 1L;
	private FestivalMobility fm;
	
	public FestivalMobilityComponent(FestivalMobility festivalMobility) {
		this.fm = festivalMobility;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		Color[] colors = { Color.red, Color.blue, Color.green, Color.orange, Color.cyan, Color.darkGray, Color.yellow,
				Color.pink, Color.magenta, Color.gray };

		Host[] hosts = fm.hosts;
		Host[] travelers = fm.travelers;

		for (int i = 0; i < fm.noHosts; i++) {
			int x = (int) (hosts[i].currentX * getSize().height / fm.height);
			int y = (int) (hosts[i].currentY * getSize().width / fm.width);

			int width = 10;
			int height = 10;

//			int maxPeers = 0;
//			for (int j = 0; j < fm.noHosts; j++) {
//				// if the maximum number of peers has been reached
//				if (maxPeers == hosts[i].maxPeers) {
//					break;
//				}
//				// accept as peer only if the node is not from the same community
//				if (hosts[i].groupId == hosts[j].groupId)
//					continue;
//				
//				double currentDistance = Math.sqrt((hosts[i].currentX - hosts[j].currentX) * (hosts[i].currentX - hosts[j].currentX)
//								+ (hosts[i].currentY - hosts[j].currentY) * (hosts[i].currentY - hosts[j].currentY));
//
//				float radius;
//				// check if the 2 nodes use the same protocol
//				if (hosts[i].protocol != hosts[j].protocol) {
//					continue;
//				} else {
//					switch (hosts[i].protocol) {
//					case 0:
//						radius = FestivalMobility.BLUETOOTH_RADIUS;
//						break;
//					case 1:
//						radius = FestivalMobility.WIFIDIRECT_RADIUS;
//						break;
//					default:
//						// the nodes support both protocols
//						radius = FestivalMobility.WIFIDIRECT_RADIUS;
//					}
//				}
//				
//				// i < j ?????????
//				if (currentDistance < radius) {
//					// System.out.println("current dist = " + currentDistance + " radius = " + radius);
//					int x1 = (int) (hosts[j].currentX * getSize().height / fm.height);
//					int y1 = (int) (hosts[j].currentY * getSize().width / fm.width);
//
//					g2d.setColor(Color.black);
//					// g2d.drawLine(y + width / 2, x + height / 2, y1 + width / 2, x1 + height / 2);
//					maxPeers++;
//				}
//			}
//
//			// do the same for traveler nodes
//			for (int j = 0; j < fm.noOfTravelers; j++) {
//				double currentDistance = Math
//						.sqrt((hosts[i].currentX - travelers[j].currentX) * (hosts[i].currentX - travelers[j].currentX)
//								+ (hosts[i].currentY - travelers[j].currentY) * (hosts[i].currentY - travelers[j].currentY));
//
//				float radius;
//				// check if the 2 nodes use the same protocol
//				if (hosts[i].protocol != travelers[j].protocol) {
//					continue;
//				} else {
//					switch (hosts[i].protocol) {
//					case 0:
//						radius = FestivalMobility.BLUETOOTH_RADIUS;
//					case 1:
//						radius = FestivalMobility.WIFIDIRECT_RADIUS;
//					default:
//						// the nodes support both protocols
//						radius = FestivalMobility.WIFIDIRECT_RADIUS;
//					}
//				}
//				
//				if (currentDistance < radius) {
//					int x1 = (int) (hosts[j].currentX * getSize().height / fm.height);
//					int y1 = (int) (hosts[j].currentY * getSize().width / fm.width);
//
//					g2d.setColor(Color.black);
//					// g2d.drawLine(y + width / 2, x + height / 2, y1 + width / 2, x1 + height / 2);
//				}
//			}

			for (int j = 0; j < fm.noOfGroups; j++) {
				for (int k = 0; k < fm.groupSize; k++)
					// i + 1?
					if (i == fm.groups[j][k]) {
						g2d.setColor(colors[j % colors.length]);
						break;
					}
			}

			Ellipse2D.Double circle = new Ellipse2D.Double(y, x, width, height);
			g2d.fill(circle);
		}
		
		for (int i = 0; i < fm.noOfTravelers; i++) {
			int x = (int) (travelers[i].currentX * getSize().height / fm.height);
			int y = (int) (travelers[i].currentY * getSize().width / fm.width);

			int width = 10;
			int height = 10;
			
			// g2d.setColor(colors[i % colors.length]);
//			Ellipse2D.Double circle = new Ellipse2D.Double(y, x, width, height);
//			g2d.fill(circle);
		}

		// draw the horizontal lines of the grid.
		for (int i = 0; i < fm.rows; i++) {
			g2d.setColor(Color.lightGray);
			g2d.drawLine(0, (int) (i * (double) getSize().height / (double) fm.rows), getSize().width,
					(int) (i * (double) getSize().height / (double) fm.rows));
		}

		// draw the vertical lines of the grid.
		for (int i = 0; i < fm.cols; i++) {
			g2d.setColor(Color.lightGray);
			g2d.drawLine((int) (i * (double) getSize().width / (double) fm.cols), 0,
					(int) (i * (double) getSize().width / (double) fm.cols), getSize().height);
		}
	}
}

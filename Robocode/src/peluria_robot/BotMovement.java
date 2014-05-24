package peluria_robot;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

public interface BotMovement {
	public void init();
	public void run();
	public void onScannedRobot(ScannedRobotEvent event);
	public void onHitByBullet(HitByBulletEvent event);
	public void onRobotDeath(RobotDeathEvent e) ;
	public void onPaint(Graphics2D g);
	public void setTarget(String name);

}

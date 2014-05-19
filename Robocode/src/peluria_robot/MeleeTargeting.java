package peluria_robot;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class Enemy {
	String name;
	double currentEnergy;
	double distance;
	double bearing;
	double heading;
	double velocity;
	boolean isSentryRobot;

	// 1 not important 2 normal 3 dangerous
	int priority = 2;

	Enemy(String name, double currentEnergy, double distance, double bearing,
			double heading, double velocity, boolean isSentryRobot) {
		this.name = name;
		this.currentEnergy = currentEnergy;
		this.distance = distance;
		this.bearing = bearing;
		this.heading = heading;
		this.velocity = velocity;
		this.isSentryRobot = isSentryRobot;
	}

	public Enemy() {
		name = "";
		currentEnergy = distance = bearing = heading = velocity = 0.0;
		isSentryRobot = false;
	}
}

public class MeleeTargeting {

	PeluriaRobot pr;
	Map<String, Enemy> mapOfEnemy;
	Enemy target;
	GuessFactorTargeting gft;
	int direction = 1;

	double energyThreshold = 10;
	double distanceThreshold = 10;
	double dangerousThreshold = 20;

	Point2D.Double myLocation = new Point2D.Double();

	public MeleeTargeting(PeluriaRobot pr, GuessFactorTargeting gft) {
		this.pr = pr;
		mapOfEnemy = new HashMap<String, Enemy>();
		target = new Enemy();
		this.gft = gft;

	}

	private boolean existsTheDangerousEnemy() {
		for(Enemy e: mapOfEnemy.values()){
			if(e.priority == 3)
				return true;
		}
		return false;
	}
	
	double getBulletPower(double distance) {
		double maxPower = 3;
		if (pr.getEnergy() < energyThreshold)
			maxPower = 0.2;

		return (1 - (distance / Math
				.sqrt(Math.pow(pr.getBattleFieldHeight(), 2)
						+ Math.pow(pr.getBattleFieldWidth(), 2))))
				* maxPower;
	}

	private void update(ScannedRobotEvent event) {
		for (Enemy e : mapOfEnemy.values()) {
			if (event.getName() == e.name) {
				if (event.getDistance() != e.distance) {
					e.distance = event.getDistance();
				}
				if (event.getEnergy() <= dangerousThreshold) {
					e.priority = 1; // not very dangerous
				}
			}
		}

	}
	
	private void getTheClosestEnemy(ScannedRobotEvent event, boolean exists) {
		if(exists){
			for (Enemy e : mapOfEnemy.values()) {
				if(e.name == event.getName() && e.priority == 3 && e.distance < target.distance + distanceThreshold){
					target = e;
				}
			}	
		}else{
			for (Enemy e : mapOfEnemy.values()) {
				if (e.distance < target.distance)
					target = e;
			}
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent event) {
		if (event.getTime() < 8.0) {
			Enemy enemy = null;
			enemy = new Enemy(event.getName(), event.getEnergy(),
					event.getDistance(), event.getBearing(),
					event.getHeading(), event.getVelocity(),
					event.isSentryRobot());
			mapOfEnemy.put(enemy.name, enemy);
		} else {
			if(event.getTime() == 8.0)
				target = mapOfEnemy.values().iterator().next(); // give the first at start.

			update(event); // update the distance of the current robot scanned
			boolean exists = existsTheDangerousEnemy();
			getTheClosestEnemy(event, exists); // Set target to the closest robot

			ScannedRobotEvent enemy = new ScannedRobotEvent(target.name,
					target.currentEnergy, target.bearing, target.distance,
					target.heading, target.velocity, target.isSentryRobot);

			System.out.println("TARGET: " + enemy.getName());

			// Una volta identificato SPARAGLI
			gft.onScannedRobot(enemy);
		}

	}

	public void onHitByBullet(HitByBulletEvent event) {
		if (target.name != event.getName()) {
			getTheCurrentEnemy(event.getName());
		}

	}

	private void getTheCurrentEnemy(String name) {
		for (Enemy e : mapOfEnemy.values()) {
			if (name == e.name) {
				if (e.distance <= target.distance + distanceThreshold)
					e.priority = 3; // /dangerous;
					target = e;
			}

		}
	}

	public void onRobotDeath(RobotDeathEvent e) {
		mapOfEnemy.remove(e.getName());
	}

	public void onPaint(Graphics2D g) {
	}

}

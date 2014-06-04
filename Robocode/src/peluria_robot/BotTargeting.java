package peluria_robot;

import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

//This class represent an Enemy, with all the variable that the PR receive from the environment
class Enemy {
	String name;
	double currentEnergy;
	double distance;
	double bearing;
	double heading;
	double velocity;

	// True if the enemy hit Peluria-Bot
	boolean priority = false;
	long timeFired;

	// This is a constructor
	Enemy(String name, double currentEnergy, double distance, double bearing, double heading, double velocity, boolean isSentryRobot) {
		this.name = name;
		this.currentEnergy = currentEnergy;
		this.distance = distance;
		this.bearing = bearing;
		this.heading = heading;
		this.velocity = velocity;
	}

	// this too
	public Enemy() {
		name = "";
		currentEnergy = distance = bearing = heading = velocity = 0.0;
	}
}

public class BotTargeting {

	PeluriaRobot pr;

	// This is the map that contains the current enemies of the battle
	Map<String, Enemy> mapOfEnemy;
	// This is the map that contains the current enemies's Guess Factor for the
	// targeting
	Map<String, GuessFactorTargeting> mapOfGuessFactor;
	// this is THE enemy that we want to fire.
	Enemy target;
	// This is the class of the MELEE Movement, with the strategy Minimum Risk
	BotMovement botMovement;

	// weight to give at the distance enemy
	double distanceThreshold = 3;
	// This one is relative at the potentially dangerous robot, because we want
	// to kill the enemy that have the energy less than
	// this threshold
	double dangerousThreshold = 20;

	// Time elapsed between fires, if the enemy don't shoot Peluria-Bot we have
	// to set priority false
	double timeFiredThreshold = 150;

	public BotTargeting(PeluriaRobot pr, BotMovement mrm) {
		this.pr = pr;
		mapOfEnemy = new HashMap<String, Enemy>();
		mapOfGuessFactor = new HashMap<String, GuessFactorTargeting>();
		target = new Enemy();
		this.botMovement = mrm;
	}

	// This function at the first scanned all robot of the battle
	public void onScannedRobot(ScannedRobotEvent event) {
		// need 8 tick to scanned all robot
		if (event.getTime() < 8.0) {
			Enemy enemy = null;
			enemy = new Enemy(event.getName(), event.getEnergy(), event.getDistance(), event.getBearingRadians(), event.getHeadingRadians(), event.getVelocity(), event.isSentryRobot());
			mapOfEnemy.put(enemy.name, enemy);
			mapOfGuessFactor.put(enemy.name, new GuessFactorTargeting(pr));
		} else {
			// Sometimes occurs that one enemy is not scanned. In this case
			// check if there are already in the maps.
			// If this is not present jet, I put him in the map.
			if (!mapOfEnemy.containsKey(event.getName())) {
				Enemy enemy = null;
				enemy = new Enemy(event.getName(), event.getEnergy(), event.getDistance(), event.getBearingRadians(), event.getHeadingRadians(), event.getVelocity(), event.isSentryRobot());
				mapOfEnemy.put(enemy.name, enemy);
				mapOfGuessFactor.put(enemy.name, new GuessFactorTargeting(pr));
			}
			// This function update the values of current scanned enemy.
			update(event);

			target = getTarget();

			// When I know who's the target, I fire
			if (target.name == event.getName()) {
				for (String name : mapOfGuessFactor.keySet()) {
					if (name == target.name) {
						mapOfGuessFactor.get(name).onScannedRobot(event);
					}
				}
			}
		}

	}

	private Enemy getTarget() {

		if (mapOfEnemy.size() == 1) {
			return mapOfEnemy.values().iterator().next();
		}

		Enemy enemyToReturn = getTheClosestEnemyThatFireMe();
		// No enemy fire Peluria-Bot
		if (enemyToReturn == null) {
			// Get the enemy with the energy is lower than threshold
			enemyToReturn = getLowEnemy();
			if (getLowEnemy() == null)
				enemyToReturn = getTheClosestEnemy();
		}
		botMovement.setTarget(enemyToReturn.name);
		return enemyToReturn;
	}

	// Return the Enemy with the energy lower than threshold
	private Enemy getLowEnemy() {
		Enemy enemyToReturn = null;
		for (Enemy e : mapOfEnemy.values()) {
			if (e.currentEnergy < dangerousThreshold) {
				if (enemyToReturn != null) {
					if (enemyToReturn.distance > e.distance) {
						enemyToReturn = e;
					}
				} else {
					enemyToReturn = e;
				}
			}

		}
		return enemyToReturn;
	}

	private void update(ScannedRobotEvent event) {
		for (Enemy e : mapOfEnemy.values()) {
			if (event.getName() == e.name) {
				e.bearing = event.getBearingRadians();
				e.heading = event.getHeadingRadians();
				e.currentEnergy = event.getEnergy();
				e.velocity = event.getVelocity();
				e.distance = event.getDistance();
			}

			if (pr.getTime() - e.timeFired > timeFiredThreshold)
				e.priority = false;

		}

	}

	// Coefficent based on the distance of the enemy and the energy
	private double getCoefficentTarget(Enemy e) {
		return distanceThreshold / e.distance + 1 / e.currentEnergy;
	}

	// Get the best enemy to shoot among the enemy that fire me
	private Enemy getTheClosestEnemyThatFireMe() {
		Enemy enemyToReturn = null;
		for (Enemy e : mapOfEnemy.values()) {
			if (e.priority == true) {
				if (enemyToReturn != null) {
					double coefficent_e = getCoefficentTarget(e);
					double coefficent_enemyToReturn = getCoefficentTarget(enemyToReturn);
					if (coefficent_enemyToReturn < coefficent_e) {
						enemyToReturn = e;
					}
				} else {
					enemyToReturn = e;
				}
			}
		}
		return enemyToReturn;
	}

	// Get the best enemy to shoot
	private Enemy getTheClosestEnemy() {
		Enemy enemyToReturn = target;
		double max_value = Integer.MIN_VALUE;
		for (Enemy e : mapOfEnemy.values()) {
			double coefficent_target = getCoefficentTarget(e);
			if (max_value < coefficent_target) {
				max_value = coefficent_target;
				enemyToReturn = e;

			}
		}
		return enemyToReturn;
	}

	public void onHitByBullet(HitByBulletEvent event) {
		// If someone fire me and is relatively near to me thus this is the
		// target
		if (mapOfEnemy.get(event.getName()) != null) {
			mapOfEnemy.get(event.getName()).priority = true;
			mapOfEnemy.get(event.getName()).timeFired = pr.getTime();
		}
	}

	public void onRobotDeath(RobotDeathEvent e) {
		mapOfEnemy.remove(e.getName());
		target = getTarget();
	}

	public void onPaint(Graphics2D g) {

		for (String name : mapOfGuessFactor.keySet()) {
			if (name == target.name) {
				mapOfGuessFactor.get(name).onPaint(g);
			}
		}
	}

}

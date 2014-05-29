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
	boolean isSentryRobot;

	// 1 not very dangerous 2 normal 3 dangerous
	boolean priority = false;
	long timeFired;
	

	// This is a constructor
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

	// this too
	public Enemy() {
		name = "";
		currentEnergy = distance = bearing = heading = velocity = 0.0;
		isSentryRobot = false;
	}
}

public class MeleeTargeting {

	PeluriaRobot pr;

	// This is the map that contains the current enemies of the battle
	Map<String, Enemy> mapOfEnemy;
	// This is the map that contains the current enemies's Guess Factor for the
	// targeting
	Map<String, GuessFactorTargeting> mapOfGuessFactor;
	// this is THE enemy that we want to fire.
	Enemy target;
	// This is the class of the MELEE Movement, with the strategy Minimum Risk
	BotMovement mrm;

	// Here, we have two threshold.
	double distanceThreshold = 3;
	// This one is relative at the potentially dangerous robot, because we want
	// to (must to decide)ignore/kill the enemy that have the energy less than
	// this threshold
	double dangerousThreshold = 20;
	
	double timeFiredThreshold = 150;

	public MeleeTargeting(PeluriaRobot pr, BotMovement mrm) {
		this.pr = pr;
		mapOfEnemy = new HashMap<String, Enemy>();
		mapOfGuessFactor = new HashMap<String, GuessFactorTargeting>();
		target = new Enemy();
		this.mrm = mrm;
	}

	/** onScannedRobot PART **/

	// This function at the first scanned all robot of the battle. Know that we
	// need 8 tick to scanned all robot (we're not optimist, will say later why)
	// Later perform the current Target. At the first is the nearest in the
	// battle to Peluria Robot.
	// Later is the most dangerous (this is perform in the function
	// getTheClosestEnemy( ... )
	// When I choose the target I set his GuessFactor and fire against him
	public void onScannedRobot(ScannedRobotEvent event) {
		if (event.getTime() < 8.0) {
			Enemy enemy = null;
			enemy = new Enemy(event.getName(), event.getEnergy(),
					event.getDistance(), event.getBearingRadians(),
					event.getHeadingRadians(), event.getVelocity(),
					event.isSentryRobot());
			mapOfEnemy.put(enemy.name, enemy);
			mapOfGuessFactor.put(enemy.name, new GuessFactorTargeting(pr));
		} else {
			// Sometimes occurs that one enemy is not scanned. In this case
			// check if there are already in the maps.
			// If this is not present jet, I put him in the map.
			if (!mapOfEnemy.containsKey(event.getName())) {
				Enemy enemy = null;
				enemy = new Enemy(event.getName(), event.getEnergy(),
						event.getDistance(), event.getBearingRadians(),
						event.getHeadingRadians(), event.getVelocity(),
						event.isSentryRobot());
				mapOfEnemy.put(enemy.name, enemy);
				mapOfGuessFactor.put(enemy.name, new GuessFactorTargeting(pr));
			}
			// This function update the values of current scanned enemy.
			update(event);

			target = getTarget();

			/** SOME STAMPS FOR DEBUGGING **/
//			for (Enemy e : mapOfEnemy.values()) {
//				System.out.println(e.name + " PRIORITY: " + e.priority);
//			}
//			System.out.println("TARGET: " + target.name);
			/** END **/

			// When I know who's the target, I fire him.
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
		if (enemyToReturn == null) {
			if (getLowEnemy() != null) {
				enemyToReturn = getLowEnemy();
			} else
				enemyToReturn = getTheClosestEnemy();
		}
//		System.out.println(enemyToReturn.name);
		mrm.setTarget(enemyToReturn.name);
		return enemyToReturn;
	}

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
			
			if(pr.getTime() - e.timeFired > timeFiredThreshold)
				e.priority=false;
			
		}

	}

	private double getCoefficentTarget(Enemy e){
		return distanceThreshold / e.distance + 1 / e.currentEnergy;
	}
	
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

	/** END onScannedRobot part **/

	/** onHitBullet part **/

	public void onHitByBullet(HitByBulletEvent event) {
		// If someone fire me and is relatively near to me thus this is the
		// target
		getTheCurrentEnemy(event.getName());
	}

	private void getTheCurrentEnemy(String name) {
		if(mapOfEnemy.get(name) != null){
			mapOfEnemy.get(name).priority = true;
			mapOfEnemy.get(name).timeFired=pr.getTime();
		}
	}

	/** END onHitBullet part **/

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

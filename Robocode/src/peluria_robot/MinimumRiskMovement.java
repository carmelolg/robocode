package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


public class MinimumRiskMovement implements BotMovement {

	// Map Name , Enemy
	HashMap<String, EnemyInfo> enemies = new HashMap<String, EnemyInfo>();
	// The current enemy target
	EnemyInfo target;
	
	// Next location to arrive
	Point2D.Double nextLocation;
	// Previous location
	Point2D.Double lastLocation;
	// Current location
	Point2D.Double myLocation;
	// Actual energy
	double myEnergy;

	// The offset of distance between current location and next location
	// If distance of myLocation and nextLocation > of DISTANCE_NEXT_OFFSET choose new nextLocation
	final double DISTANCE_NEXT_OFFSET = 15;
	// Point generated each time choose nextLocation
	final double POINT_GEN = 1000;
	// Minimum distance between Peluria-Bot and target (percentage)
	final double MIN_DISTANCE_TARGET = 0.5;
	// Minimum distance between the next location to choose and current location
	final double MIN_DISTANCE_MYLOCATION = 50;
	// Maximum distance to myLocation and point generated
	final double DISTANCE_MYLOCATION = 150;

	// Battlefield
	static Rectangle2D.Double battleField;
	
	// Peluria-Bot
	PeluriaRobot pr;

	public MinimumRiskMovement(PeluriaRobot pr) {
		this.pr = pr;
	}

	public void init() {
		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		target = new EnemyInfo();
		nextLocation = myLocation;
		lastLocation = myLocation;
		// Set battlefield lower than real battlefield to avoid wall 
		battleField = new Rectangle2D.Double(50, 50, pr.getBattleFieldWidth() - 100, pr.getBattleFieldHeight() - 100);
		
		// Set Peluria-Bot color
		pr.setColors(Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY);
		pr.setScanColor(Color.red);
	}


	// Call in loop in Peluria-Bot run()
	public void run() {

		// Take energy and location
		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		myEnergy = pr.getEnergy();
		
		// Move radar
		pr.setTurnRadarRightRadians(Double.POSITIVE_INFINITY  );
		
		// If target is live Move
		if (target.live) {
			doMovement();
		}
	}

	// Move Method
	private void doMovement() {

		// Distance to the next location
		double distanceToNextDestination = myLocation.distance(nextLocation);

		// search a new destination if Peluria-Bot reached nextDestination
		if (distanceToNextDestination < DISTANCE_NEXT_OFFSET) {

			//Generate new destination
			nextLocation = getNextPoint();

			// Save last location
			lastLocation = myLocation;

		} else {

			// Go to the current destination
			
			// Calculate the angle to reach the destination
			double angle = TriUtil.absoluteBearing(myLocation, nextLocation) - pr.getHeadingRadians();
			
			// Calculate the direction
			int direction = 1;
			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}
			

			// Go to the direction
			pr.setAhead(distanceToNextDestination * direction);
			angle = Utils.normalRelativeAngle(angle);
			pr.setTurnRightRadians(angle );
			
			// Set the velocity based on the angle to turn
			pr.setMaxVelocity(TriUtil.limit(0,(1 - (Math.abs(angle)  / Math.PI) ) * 8.0, 8.0));

		}

	}

	// Generate the point and choose the best
	public Point2D.Double getNextPoint() {
		// Point generated
		Point2D.Double testPoint =null;
		// Best point generated
		Point2D.Double bestPoint = null;
		// Distance to the current taget
		double distanceToTarget = myLocation.distance(target.location);
		
		// Generation of points
		for(int i=0;i<POINT_GEN;i++){
			// Generate the point and verify if is the best
			testPoint = generatePoint(distanceToTarget);
			if (battleField.contains(testPoint) && (bestPoint == null || riskEvaluation(testPoint) < riskEvaluation(bestPoint))) {
				bestPoint = testPoint;
			}
		} 

		return bestPoint;
	}


	// Generate the point projecting the current location in :
	// random angle between 0 and 2*Math.PI(360)
	// random distance between MIN_DISTANCE_MYLOCATION and DISTANCE_MYLOCATION
	// if the distance is > than the MIN_DISTANCE_TARGET % than take the last as distance
	public Point2D.Double generatePoint(double distanceToTarget) {
		return TriUtil.project(myLocation, 2 * Math.PI * Math.random(), Math.min(distanceToTarget * MIN_DISTANCE_TARGET, MIN_DISTANCE_MYLOCATION + DISTANCE_MYLOCATION * Math.random()));
	}

	//
	public double riskEvaluation(Point2D.Double point) {

		// Coulomb's law , The force of attraction is inversely proportional to
		// the square of the distance
		// The risk start to the inverse of distance with last location and the target location
		// Last location because Peluria-Bot have to avaid to stay in the same place much time
		double eval = 1 / point.distanceSq(lastLocation) + 1 / point.distanceSq(target.location);

		
		for (String key : enemies.keySet()) {
			EnemyInfo en = enemies.get(key);
			// For each enemy:
			// - Math.min(en.energy/myEnergy,2) is an indicator how dangerous is enemy
			// - Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p)))
			// is bigger if the moving direction is parallel to the enemy, Peluria-Bot tries move perpendicular
			// - en.damage / p.distanceSq(en.pos) proportionally to the distance and the damage caused by the enemy
			if (en.live) {
				eval += Math.min(en.energy / myEnergy, 2) * (1 + Math.abs(Math.cos(TriUtil.absoluteBearing(point, myLocation) - TriUtil.absoluteBearing(point, en.location)))) *
						en.damage / point.distanceSq(en.location);
			}
		}
		return eval;
	}

	// When scanned robot
	public void onScannedRobot(ScannedRobotEvent e) {

		// Take or create EnemyInfo of the enemy
		EnemyInfo en = enemies.get(e.getName());
		if (en == null) {
			en = new EnemyInfo();
			enemies.put(e.getName(), en);
		}

		// Update the information
		en.energy = e.getEnergy();
		en.live = true;
		en.bearing = pr.getHeadingRadians() + e.getBearingRadians();
		en.location = TriUtil.project(myLocation, pr.getHeadingRadians() + e.getBearingRadians(), e.getDistance());

		//FIXME target change based of the targetting
		if (!target.live || e.getDistance() < myLocation.distance(target.location)) {
			target = en;
		}

		// Change in one vs one mode movement
		if (pr.getOthers() == 1)
			pr.changeWaveMovement();
	}

	// When enemy dies
	public void onRobotDeath(RobotDeathEvent e) {
		enemies.get(e.getName()).live = false;
	}

	// When Peluria-Bot is hit by bullet
	public void onHitByBullet(HitByBulletEvent event) {
		enemies.get(event.getName()).damage=enemies.get(event.getName()).damage+0.1;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.drawRect((int)nextLocation.x, (int)nextLocation.y, 10, 10);
		
		g.setColor(Color.RED);
		g.drawRect((int)target.location.x, (int)target.location.y, 10, 10);
		
		
		ArrayList<Point2D.Double> points=new ArrayList<Point2D.Double>();
				
		Point2D.Double testPoint;
		double max=0;
		double distanceToTarget = myLocation.distance(target.location);
		int i=0;
		
		do {
			testPoint = generatePoint(distanceToTarget);
			if(battleField.contains(testPoint) ) {
				points.add(testPoint);
				if(riskEvaluation(testPoint)>max)
					max=riskEvaluation(testPoint);
			}
		} while(i++ < POINT_GEN);
		
		
		for(Point2D.Double p : points){
			int colorRisk=(int)(riskEvaluation(p)/max * 255);
			g.setColor(new Color(colorRisk,0,255-colorRisk));
			g.fillRect((int)p.x, (int)p.y, 5, 5);
		}
	}
	
	public void setTarget(Point2D.Double targetLocation){
		this.target.location=targetLocation;
	}
	
	
}

class EnemyInfo {
	public Point2D.Double location;
	public double energy;
	public boolean live;
	public double bearing;
	public double damage;
}

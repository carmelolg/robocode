package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class EnemyMovement{
	double velocity;
	double heading;
	Point2D.Double location;
	
	double dVelocity;
	double dHeading;
	double dDistance;

	
	public EnemyMovement(double velocity, double heading, Double location, double dVelocity, double dHeading, double dDistance) {
		super();
		this.velocity = velocity;
		this.heading = heading;
		this.location = location;
		this.dVelocity = dVelocity;
		this.dHeading = dHeading;
		this.dDistance = dDistance;
	}


	double compare(EnemyMovement mov){
		double maxVelocity=8.0;
		double maxHeading = (10 - 0.75 * Math.abs(velocity));
		double maxDistance = velocity * PatternMatching.TICK_SCAN;
		
		
		double distanceVelocity=Math.pow(Math.abs(dVelocity/maxVelocity) - Math.abs(mov.dVelocity/maxVelocity), 2);
		double distanceHeading=Math.pow(dHeading/maxHeading - mov.dHeading/maxHeading, 2);
		double distanceDistance =Math.pow(dDistance/maxDistance - mov.dDistance/maxDistance, 2);
		
		System.out.println(distanceVelocity+" "+distanceHeading+" "+distanceDistance);
		
		return Math.sqrt(distanceVelocity+ distanceHeading + distanceDistance );
		
	}
	
	
	
	
}


public class PatternMatching {
	
	static ArrayList<EnemyMovement> logEnemy=new ArrayList<EnemyMovement>();
	PeluriaRobot pr;
	
	public PatternMatching(PeluriaRobot pr) {
		this.pr=pr;
	}
	
	
	final static int TICK_SCAN = 5;
		
	final int LAST_MOVEMENT_SIZE = 5;
	
	Point2D.Double enemyLocation;
	Point2D.Double enemyFutureLocation;
	
	public void onScannedRobot(ScannedRobotEvent e) {
		
		// Bearing betwen Peluria-Bot and enemy
		double absBearing = pr.getHeadingRadians() + e.getBearingRadians();
		// Enemy Location
		enemyLocation = TriUtil
				.project(new Point2D.Double(pr.getX(),pr.getY()), absBearing, e.getDistance());
		
		
		if(e.getTime() % TICK_SCAN == 0)
			addLogMovement(e,enemyLocation);
		
		
		int bestPattern=0;
		double bestEvaluation=Integer.MAX_VALUE;
		if(logEnemy.size()-2*LAST_MOVEMENT_SIZE+1<=0)return;
		for(int i=0;i<logEnemy.size()-2*LAST_MOVEMENT_SIZE+1;i++){
			double evaluationPattern=evaluate(i);
			
			if(evaluationPattern < bestEvaluation){
				bestEvaluation = evaluationPattern;
				bestPattern = i ;
			}
			
		}
		System.out.println("BEEEEEEEEEST "+bestEvaluation);
		
		bestPattern += LAST_MOVEMENT_SIZE;
		double power = 2;
		double powerVel = 20 -3*power;
		
		double timeImpact = e.getDistance() / powerVel;
		
		int indexFutureMovement = (int)( timeImpact / TICK_SCAN);
		
		if(bestPattern+indexFutureMovement > logEnemy.size()) return;
		
		double angleFuture=0;
		double futureDistance=0;
		for(int i=bestPattern;i<bestPattern+indexFutureMovement;i++){
			angleFuture+=logEnemy.get(i).dHeading;
			futureDistance+=logEnemy.get(i).dDistance;
		}
		



		
		enemyFutureLocation=TriUtil.project(enemyLocation, angleFuture, futureDistance);
		double absFutureBearing =TriUtil.absoluteBearing(pr.getX(), pr.getY(), enemyFutureLocation.x, enemyFutureLocation.y);

		
		double gunAdjust = Utils.normalRelativeAngle(absFutureBearing
				- pr.getGunHeadingRadians() );

		pr.setTurnGunRightRadians(gunAdjust);
		
		if (pr.getGunHeat() == 0 &&  Math.abs(pr.getGunTurnRemaining()) < 10)
			pr.setFireBullet(power) ;
	}
	
	



	private double evaluate(int i) {
		
		double evaluation=0;
		for(int j=i;j<LAST_MOVEMENT_SIZE+i;j++){
			
			EnemyMovement movementToEvaluate=logEnemy.get(j);
			EnemyMovement currentEvaluate=logEnemy.get(logEnemy.size() - LAST_MOVEMENT_SIZE + (j-i) );
			
			evaluation+=currentEvaluate.compare(movementToEvaluate);
		}
		
		return evaluation;
	}



	public void addLogMovement(ScannedRobotEvent e,Point2D.Double enemyLocation) {
		double deltaVelocity = 0;
		double deltaHeading = 0;
		double deltaDistance =0;


		
		if(logEnemy.size()>0){
			EnemyMovement lastMovement=logEnemy.get(logEnemy.size()-1);
			deltaVelocity = Math.abs(lastMovement.velocity - e.getVelocity());
			deltaHeading = Math.abs(lastMovement.heading - e.getHeadingRadians());
			deltaDistance = Math.abs(lastMovement.location.distance(enemyLocation));
		}				
		
		EnemyMovement movement = new EnemyMovement(e.getVelocity(), e.getHeadingRadians(), enemyLocation, deltaVelocity, deltaHeading, deltaDistance);
		
		logEnemy.add(movement);
	}





	public void onRobotDeath(RobotDeathEvent e) {
		
	}





	public void onPaint(Graphics2D g) {
		if(logEnemy.size()<LAST_MOVEMENT_SIZE)return;
		
		g.setColor(Color.RED);
		
		g.drawRect((int)enemyFutureLocation.x, (int)enemyFutureLocation.y, 10, 10);
		
		Point2D.Double location=enemyLocation;
		for(int i=0;i<LAST_MOVEMENT_SIZE;i++){
			EnemyMovement movement=logEnemy.get(logEnemy.size()-1-i);
			location=TriUtil.project(location, movement.dHeading, movement.dDistance);
			
			g.setColor(Color.GREEN);
			
			g.drawRect((int)movement.location.x, (int)movement.location.y, 10, 10);
		}
		
	}



	public void onHitByBullet(HitByBulletEvent event) {
		
	}

}

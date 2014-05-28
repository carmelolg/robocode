package peluria_robot;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class EnemyMovement{
	double velocity;
	double heading;
	
	double dVelocity;
	double dHeading;
	double distance;
	
	public EnemyMovement(double velocity, double heading, double dVelocity, double dHeading, double distance) {
		this.velocity = velocity;
		this.heading = heading;
		this.dVelocity = dVelocity;
		this.dHeading = dHeading;
		this.distance = distance;
	}
	
	double compare(EnemyMovement mov){
		
		double distanceVelocity=Math.pow(dVelocity - mov.dVelocity, 2);
		double distanceHeading=Math.pow(dHeading - mov.dHeading, 2);
		double distanceDistance =Math.pow(distance - mov.distance, 2);
		
		return Math.sqrt(distanceVelocity+ distanceHeading + distanceDistance );
		
		
	}
	
	
	
	
}


public class PatternMatching {
	
	ArrayList<EnemyMovement> logEnemy=new ArrayList<EnemyMovement>();
	PeluriaRobot pr;
	
	public PatternMatching(PeluriaRobot pr) {
		this.pr=pr;
	}
	
	
	final int TICK_SCAN = 5;
		
	final int LAST_MOVEMENT_SIZE = 10;
	
	public void onScannedRobot(ScannedRobotEvent e) {
		
		System.out.println(logEnemy.size()+" "+e.getTime());
		
		if(e.getTime() % TICK_SCAN == 0)
			addLogMovement(e);
		
		
		int bestPattern=0;
		double bestEvaluation=Integer.MAX_VALUE;
		for(int i=0;i<logEnemy.size()-2*LAST_MOVEMENT_SIZE+1;i++){
			
			double evaluationPattern=evaluate(i);
			
			if(evaluationPattern < bestEvaluation){
				bestEvaluation = evaluationPattern;
				bestPattern = i;
			}
			
		}
		
		
		double power = 2;
		double powerVel = 20 -3*power;
		
		double timeImpact = e.getDistance() / powerVel;
		
		int indexFutureMovement = (int)( timeImpact / TICK_SCAN);
		
		if(bestPattern+indexFutureMovement > logEnemy.size()-1)indexFutureMovement=logEnemy.size()-1;
		
		double angleFuture=0;
		for(int i=bestPattern;i<bestPattern+indexFutureMovement;i++){
			angleFuture+=logEnemy.get(i).dHeading;
		}
		
		// Bearing betwen Peluria-Bot and enemy
		double absBearing = pr.getHeadingRadians() + e.getBearingRadians();


		// Enemy Location
		Point2D.Double enemyLocation = TriUtil
				.project(new Point2D.Double(pr.getX(),pr.getY()), absBearing, e.getDistance());
		
		Point2D.Double futureLocation=TriUtil.project(enemyLocation, angleFuture, logEnemy.get(indexFutureMovement).distance);
		double absFutureBearing = pr.getHeadingRadians() + TriUtil.absoluteBearing(pr.getX(), pr.getY(), futureLocation.x, futureLocation.y);

		
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



	public void addLogMovement(ScannedRobotEvent e) {
		double deltaVelocity = 0;
		double deltaHeading = 0;

		
		if(logEnemy.size()>0){
			EnemyMovement lastMovement=logEnemy.get(logEnemy.size()-1);
			deltaVelocity = Math.abs(lastMovement.velocity - e.getVelocity());
			deltaHeading = Math.abs(lastMovement.heading - e.getHeadingRadians());
		}				
		
		EnemyMovement movement =new EnemyMovement(e.getVelocity(), e.getHeadingRadians(),deltaVelocity, deltaHeading, e.getDistance());
		
		logEnemy.add(movement);
	}





	public void onRobotDeath(RobotDeathEvent e) {
		
	}





	public void onPaint(Graphics2D g) {
		
	}





	public void onHitByBullet(HitByBulletEvent event) {
		
	}

}

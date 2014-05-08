package peluria_robot;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class WaveBullet {
	private double startX, startY, startBearing, power;
	private long fireTime;
	private int direction;
	private int[] returnSegment;

	public WaveBullet(double x, double y, double bearing, double power,
			int direction, long time, int[] segment) {
		startX = x;
		startY = y;
		startBearing = bearing;
		this.power = power;
		this.direction = direction;
		fireTime = time;
		returnSegment = segment;
	}

	public double getBulletSpeed() {
		return 20 - power * 3;
	}

	public double maxEscapeAngle() {
		return Math.asin(8 / getBulletSpeed());
	}

	public boolean checkHit(double enemyX, double enemyY, long currentTime) {
		// if the distance from the wave origin to our enemy has passed
		// the distance the bullet would have traveled...
		if (Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime)
				* getBulletSpeed()) {
			double desiredDirection = Math.atan2(enemyX - startX, enemyY
					- startY);
			double angleOffset = Utils.normalRelativeAngle(desiredDirection
					- startBearing);
			double guessFactor = Math.max(-1,
					Math.min(1, angleOffset / maxEscapeAngle()))
					* direction;
			int index = (int) Math.round((returnSegment.length - 1) / 2
					* (guessFactor + 1));
			returnSegment[index]++;
			return true;
		}
		return false;
	}
} // end WaveBullet class

public class GuessFactorTargeting {
	List<WaveBullet> waves = new ArrayList<WaveBullet>();
	PeluriaRobot pr;
	static int[] stats = new int[31]; // 31 is the number of unique GuessFactors
										// we're using
	// Note: this must be odd number so we can get
	// GuessFactor 0 at middle.
	int direction = 1;

	public GuessFactorTargeting(PeluriaRobot pr) {
		this.pr = pr;
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		// ...
		// (other onScannedRobot code, might be radar/movement)
		// ...
		for(int i = 0; i<stats.length; i++){
			System.out.print(stats[i]+" - ");
		}
		System.out.println();
		// Enemy absolute bearing, you can use your one if you already declare it.
		double absBearing = pr.getHeadingRadians() + e.getBearingRadians();
 
		// find our enemy's location:
		double ex = pr.getX() + Math.sin(absBearing) * e.getDistance();
		double ey = pr.getY() + Math.cos(absBearing) * e.getDistance();
 
		// Let's process the waves now:
		for (int i=0; i < waves.size(); i++)
		{
			WaveBullet currentWave = (WaveBullet)waves.get(i);
			if (currentWave.checkHit(ex, ey, pr.getTime()))
			{
				waves.remove(currentWave);
				i--;
			}
		}
 
		// Power è settata in base alla distanza tra PeluriaRobot e l'avversario.
		//Più è distante più il power sarà più piccolo cosi da avere colpi più veloci e perdere meno energia nel caso in cui
		//non si colpisce l'avversario.
		//Il valore comunque si attesterà tra 0 e 3.
		double power =  (1 - (e.getDistance() / Math.sqrt(Math.pow(pr.getBattleFieldHeight(),2)+Math.pow(pr.getBattleFieldWidth(),2))))*3;
				
		// don't try to figure out the direction they're moving 
		// they're not moving, just use the direction we had before
		if (e.getVelocity() != 0)
		{
			if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}
		int[] currentStats = stats; // This seems silly, but I'm using it to
					    // show something else later
		WaveBullet newWave = new WaveBullet(pr.getX(), pr.getY(), absBearing, power,
                        direction, pr.getTime(), currentStats);
		
		int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
		for (int i=0; i<31; i++)
			if (currentStats[bestindex] < currentStats[i])
				bestindex = i;
 
		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                        / ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
                double gunAdjust = Utils.normalRelativeAngle(
                        absBearing - pr.getGunHeadingRadians() + angleOffset);
                pr.setTurnGunRightRadians(gunAdjust);
                
                if (pr.setFireBullet(power) != null)
                    waves.add(newWave);
}
}
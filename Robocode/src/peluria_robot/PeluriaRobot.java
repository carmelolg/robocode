package peluria_robot;

import java.awt.Graphics2D;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

/**
 * @author Peluria Brothers
 */
public class PeluriaRobot extends AdvancedRobot {

	GuessFactorTargeting gft = new GuessFactorTargeting(this);
	MinimumRiskMovement wsm = new MinimumRiskMovement(this);
//	WaveSurfingMovement wsm=new WaveSurfingMovement(this);

	public void run() {
		wsm.init();

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		setTurnRadarRightRadians(2*Math.PI);
		
		for(;;){
			wsm.run();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		wsm.onScannedRobot(event);
		gft.onScannedRobot(event);
	}

	@Override
	public void onHitByBullet(HitByBulletEvent event) {
		wsm.onHitByBullet(event);
	}
	
	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		wsm.onRobotDeath(e);
	}

	
	@Override
	public void onPaint(Graphics2D g) {
		wsm.onPaint(g);
		gft.onPaint(g);
	}
}

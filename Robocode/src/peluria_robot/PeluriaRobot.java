package peluria_robot;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

/**
 * @author Peluria Brothers
 */
public class PeluriaRobot extends AdvancedRobot {

	GuessFactorTargeting gft = new GuessFactorTargeting(this);
	WaveSurfingMovement wsm = new WaveSurfingMovement(this);

	public void run() {

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		} while (true);
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

}

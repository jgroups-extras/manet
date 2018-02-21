package urv.emulator.tasks.topology;

import org.jgroups.Address;

import urv.emulator.VirtualNetworkInformation;
import urv.emulator.core.EmulationController;
import urv.emulator.tasks.EmulatorTask;

/**
 * This tasks
 * 
 * @author Marcel Arrufat Arias
 */
public class TopologyChangesTask extends EmulatorTask {

	//	CONSTRUCTORS --
	
	/**
	 * @param emulationController
	 */
	public TopologyChangesTask() {
		super();		
	}

	//	OVERRIDDEN METHODS --

	/**
	 * Add the code that should be launched in the run method
	 */
	@Override
	public void doSomething() {
		EmulationController controller = super.getEmulationController();
		VirtualNetworkInformation vni = controller.getVirtualNetworkInformation();
		int networkSize = vni.getNetworkSize();		
		TopologyChecker checker = new TopologyChecker(vni);
		int totalTime = 0;
		int step = 2;
		while (true) {
			//TODO: change by using currentTimeMillis 
			try {
				Thread.sleep(step * 1000);
				totalTime += step;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean changes = vni.performTopologyChangesUntilSecond(totalTime);
			if (changes)
				print("Topology changing!",true);
			boolean oneHopOk = false;
			boolean twoHopOk = false;

			int totalNodesOk = 0;
			// For each node, check 1-hop neighbors and 2-hop neighbors
			for (int i = 0; i < networkSize; i++) {
				Address addr = vni.getEmuNodeAddress(i + 1);
				oneHopOk = checker.checkOneHopNeighbors(addr);
				if (oneHopOk) {
					twoHopOk = checker.checkTwoHopNeighbors(addr);
				}
				if (oneHopOk && twoHopOk) {
					totalNodesOk++;
				}
			}
			float result = (float) 100 * totalNodesOk / networkSize;			 
			print("2-hop neighborhood correct at "	+ (result)
					+ "% (" + totalNodesOk + "/" + networkSize + ")",true);
		}
	}
}
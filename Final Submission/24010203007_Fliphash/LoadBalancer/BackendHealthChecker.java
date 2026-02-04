package LoadBalancer;

import fliphash.TerminalDisplayManager;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks the health of registered backend servers.
 */
public class BackendHealthChecker implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                TerminalDisplayManager.addLog("Backend health check interrupted: " + e.getMessage());
                break;
            }
            for (BackendManager.BackendInfo backend : BackendManager.getBackends()) {
                if (!BackendManager.isBackendActive(backend)) {
                    BackendManager.removeBackend(backend);
                    TerminalDisplayManager.addLog("Backend removed (inactive): " + backend);
                }
            }
        }
    }
}

package org.jolokia.docker.maven.service;/*
 * 
 * Copyright 2014 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jolokia.docker.maven.access.DockerAccess;
import org.jolokia.docker.maven.access.DockerAccessException;
import org.jolokia.docker.maven.config.*;
import org.jolokia.docker.maven.util.Logger;

import static org.jolokia.docker.maven.util.WaitUtil.sleep;

/**
* @author roland
* @since 17/12/14
*/
class ShutdownAction {

    // The image used
    private String image;

    // Alias of the image
    private final String containerId;

    // Description
    private String description;

    // How long to wait after shutdown (in milliseconds)
    private int shutdownGracePeriod;

    ShutdownAction(ImageConfiguration imageConfig, String containerId) {
        this.image = imageConfig.getName();
        this.containerId = containerId;
        this.description = imageConfig.getDescription();
        RunImageConfiguration runConfig = imageConfig.getRunConfiguration();
        WaitConfiguration waitConfig = runConfig != null ? runConfig.getWaitConfiguration() : null;
        this.shutdownGracePeriod = waitConfig != null ? waitConfig.getShutdown() : 0;
    }

    /**
     * Check whether this shutdown actions applies to the given image and/or container
     *
     * @param pImage image to check
     * @return true if this action should be applied
     */
    public boolean applies(String pImage) {
        return pImage == null || pImage.equals(image);
    }

    /**
     * Clean up according to the given parameters
     *
     * @param access access object for reaching docker
     * @param log logger to use
     * @param keepContainer whether to keep the container (and its data container)
     * @param removeVolumes whether to remove associated volumes along with the container (ignored if keepContainer is true)
     */
    public void shutdown(DockerAccess access, Logger log, boolean keepContainer, boolean removeVolumes)
            throws DockerAccessException {
        // Stop the container
        access.stopContainer(containerId);
        if (!keepContainer) {
            if (shutdownGracePeriod != 0) {
                log.debug("Shutdown: Wait " + shutdownGracePeriod + " ms before removing container");
                sleep(shutdownGracePeriod);
            }
            // Remove the container
            access.removeContainer(containerId, removeVolumes);
        }
        log.info(description + ": Stop" + (keepContainer ? "" : " and remove") + " container " +
                 containerId.substring(0, 12));
    }
}

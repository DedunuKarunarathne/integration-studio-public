/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.integrationstudio.artifact.mediator.project.nature;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.wso2.integrationstudio.artifact.mediator.Activator;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.maven.util.ProjectDependencyConstants;
import org.wso2.integrationstudio.platform.core.nature.AbstractWSO2ProjectNature;
import org.wso2.integrationstudio.utils.jdt.JavaLibraryBean;
import org.wso2.integrationstudio.utils.jdt.JavaLibraryUtil;

public class CustomMediatorProjectNature extends AbstractWSO2ProjectNature {

	private static IIntegrationStudioLog log=Logger.getLog(Activator.PLUGIN_ID);
	
	public void configure() {
		try {
			this.updatePom();
		} catch (Exception e) {
			 log.error(e);
		}
	}
	
	public void updatePom() throws Exception {

		File mavenProjectPomLocation = getProject().getFile("pom.xml").getLocation().toFile();
		MavenProject mavenProject = MavenUtils.getMavenProject(mavenProjectPomLocation);
		mavenProject.getModel().getProperties().put("CApp.type", "lib/synapse/mediator");
		List<Dependency> dependencyList = new ArrayList<Dependency>();
		Map<String, JavaLibraryBean> dependencyInfoMap = JavaLibraryUtil.getDependencyInfoMap(getProject());
		Map<String, String> map = ProjectDependencyConstants.DEPENDENCY_MAP;
		for (JavaLibraryBean bean : dependencyInfoMap.values()) {
			if (bean.getVersion().contains("${")) {
				for (String path : map.keySet()) {
					bean.setVersion(bean.getVersion().replace(path,
							map.get(path)));
				}
			}
			if (!isSynapseCore(bean) && !isAxis2(bean)) {
			    Dependency dependency = new Dependency();
			    dependency.setArtifactId(bean.getArtifactId());
			    dependency.setGroupId(bean.getGroupId());
			    dependency.setVersion(bean.getVersion());
			    dependencyList.add(dependency);
			}
		}
		MavenUtils.addMavenDependency(mavenProject, dependencyList);
		MavenUtils.saveMavenProject(mavenProject, mavenProjectPomLocation);
	}

	
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	private boolean isSynapseCore(JavaLibraryBean bean) {
	    return bean.getGroupId().equals("org.apache.synapse") && bean.getArtifactId().equals("synapse-core");
	}

	private boolean isAxis2(JavaLibraryBean bean) {
	    return bean.getGroupId().equals("org.apache.axis2.wso2") && bean.getArtifactId().equals("axis2");
	}
}

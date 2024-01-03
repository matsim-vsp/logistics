package org.matsim.freight.logistics.example.lsp.lspScoring;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.freight.logistics.example.lsp.lspScoring.ExampleLSPScoring;
import org.matsim.testcases.MatsimTestUtils;

public class ExampleLSPScoringTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain() {

		Config config = ExampleLSPScoring.prepareConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ExampleLSPScoring.prepareScenario(config);

		Controler controler = ExampleLSPScoring.prepareControler(scenario);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		for (LSP lsp : LSPUtils.getLSPs(scenario).getLSPs().values()) {
			Assertions.assertEquals(13.245734044444207, lsp.getSelectedPlan().getScore(), Double.MIN_VALUE);
		}

	}
}

package net.fabricmc.minecraft.test.plugin;

import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.plugin.MuonLoaderPlugin;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;

public class TestLoaderPlugin implements MuonLoaderPlugin {

    private static final boolean DISABLE_QFAPI = Boolean.getBoolean("quick_test_disable_qfapi");

    private MuonPluginContext context;

    @Override
    public void load(MuonPluginContext ctx, Map<String, LoaderValue> dataOut) {
        context = ctx;
    }

    @Override
    public void unload(Map<String, LoaderValue> dataIn) {

    }

    @Override
    public void onLoadOptionAdded(LoadOption option) {
        if (!DISABLE_QFAPI) {
            return;
        }
        if (option instanceof ModLoadOption) {
            ModLoadOption mod = (ModLoadOption) option;
            String source = context.manager().describePath(mod.from());
            if (source.contains("qfapi-")) {
                context.ruleContext().removeOption(mod);
            }
        }
    }

}

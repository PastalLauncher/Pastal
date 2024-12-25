import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.components.auth.MicrosoftLogin;

import java.io.File;
import java.util.function.Consumer;

public class MSTest {
    @Test
    @SneakyThrows
    public void test(){
        Launcher launcher = new Launcher(new File(".minecraft"), new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
            }
        });

        launcher.setup();

        System.out.println(launcher.getComponentManager().get(MicrosoftLogin.class).getAuthUrl());

        Thread.sleep(100000);
    }
}

package hs.mediasystem;

import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javafx.application.Application;
import javafx.stage.Stage;
import net.sf.jtmdb.GeneralSettings;

public class FrontEnd extends Application {
  private static final Ini INI = new Ini(new File("mediasystem.ini"));

  private ControllerFactory controllerFactory;
  
  @Override
  public void init() throws Exception {
    Section section = INI.getSection("general");
    
//    Path moviesPath = Paths.get(section.get("movies.path"));
//    Path seriesPath = Paths.get(section.get("series.path"));
//    String sublightKey = section.get("sublight.key");
//    String sublightClientName = section.get("sublight.client");
    
    GeneralSettings.setApiKey(section.get("jtmdb.key"));
    GeneralSettings.setLogEnabled(true);
    GeneralSettings.setLogStream(System.out);
    
    String factoryClassName = section.getDefault("player.factoryClass", "hs.mediasystem.players.vlc.VLCControllerFactory");
    
    controllerFactory = (ControllerFactory) Class.forName(factoryClassName).newInstance();
  }
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();
    
    int screen = Integer.parseInt(INI.getSection("general").getDefault("screen", "0"));
    GraphicsDevice graphicsDevice = (screen >= 0 && screen < gs.length) ? gs[screen] : gs[0];
    
    System.out.println("Using display: " + graphicsDevice + "; " + graphicsDevice.getDisplayMode().getWidth() + "x" + graphicsDevice.getDisplayMode().getHeight() + "x" + graphicsDevice.getDisplayMode().getBitDepth() + " @ " + graphicsDevice.getDisplayMode().getRefreshRate() + " Hz");
    
    ProgramController controller = controllerFactory.create(INI, graphicsDevice);
    
    controller.showMainScreen();
  }
}

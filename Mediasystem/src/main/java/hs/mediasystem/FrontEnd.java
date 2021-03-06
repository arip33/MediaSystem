package hs.mediasystem;

import hs.mediasystem.beans.BeanUtils;
import hs.mediasystem.dao.IdentifierDao;
import hs.mediasystem.dao.ItemsDao;
import hs.mediasystem.dao.Setting.PersistLevel;
import hs.mediasystem.db.ConnectionPool;
import hs.mediasystem.db.DatabaseObject;
import hs.mediasystem.db.DatabaseStatementTranslator;
import hs.mediasystem.db.DatabaseUpdater;
import hs.mediasystem.db.SimpleConnectionPoolDataSource;
import hs.mediasystem.db.SimpleDatabaseStatementTranslator;
import hs.mediasystem.entity.CachingEntityFactory;
import hs.mediasystem.entity.EntityFactory;
import hs.mediasystem.entity.EntityProvider;
import hs.mediasystem.framework.Identifier;
import hs.mediasystem.framework.IdentifierProvider;
import hs.mediasystem.framework.Media;
import hs.mediasystem.framework.MediaData;
import hs.mediasystem.framework.MediaDataPersister;
import hs.mediasystem.framework.MediaDataProvider;
import hs.mediasystem.framework.MediaItemConfigurator;
import hs.mediasystem.framework.PersisterProvider;
import hs.mediasystem.framework.Person;
import hs.mediasystem.framework.PersonProvider;
import hs.mediasystem.framework.PlaybackOverlayView;
import hs.mediasystem.framework.SettingsStore;
import hs.mediasystem.framework.player.PlayerFactory;
import hs.mediasystem.persist.PersistQueue;
import hs.mediasystem.screens.AbstractSetting;
import hs.mediasystem.screens.Location;
import hs.mediasystem.screens.LocationHandler;
import hs.mediasystem.screens.MainMenuExtension;
import hs.mediasystem.screens.MainScreenLocation;
import hs.mediasystem.screens.MainScreenPresentation;
import hs.mediasystem.screens.MediaNodeCell;
import hs.mediasystem.screens.MediaNodeCellProvider;
import hs.mediasystem.screens.MessagePaneTaskExecutor;
import hs.mediasystem.screens.PlaybackLocation;
import hs.mediasystem.screens.PlaybackOverlayPane;
import hs.mediasystem.screens.PlaybackOverlayPresentation;
import hs.mediasystem.screens.PlayerPresentation;
import hs.mediasystem.screens.PluginTracker;
import hs.mediasystem.screens.Presentation;
import hs.mediasystem.screens.ProgramController;
import hs.mediasystem.screens.Setting;
import hs.mediasystem.screens.SettingGroup;
import hs.mediasystem.screens.StandardCell;
import hs.mediasystem.screens.optiondialog.BooleanOption;
import hs.mediasystem.screens.optiondialog.Option;
import hs.mediasystem.screens.selectmedia.DetailPane;
import hs.mediasystem.screens.selectmedia.DetailPaneDecorator;
import hs.mediasystem.screens.selectmedia.DetailPaneDecoratorFactory;
import hs.mediasystem.screens.selectmedia.MediaDetailPaneDecorator;
import hs.mediasystem.screens.selectmedia.PersonDetailPaneDecorator;
import hs.mediasystem.screens.selectmedia.SelectMediaLocation;
import hs.mediasystem.screens.selectmedia.SelectMediaPresentation;
import hs.mediasystem.screens.selectmedia.SelectMediaPresentationProvider;
import hs.mediasystem.screens.selectmedia.SelectMediaView;
import hs.mediasystem.screens.selectmedia.StandardView;
import hs.mediasystem.util.DuoWindowSceneManager;
import hs.mediasystem.util.SceneManager;
import hs.mediasystem.util.StringBinding;
import hs.mediasystem.util.TaskExecutor;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.stage.Stage;

import javax.sql.ConnectionPoolDataSource;

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

public class FrontEnd extends Application {
  private static final Ini INI = new Ini(new File("mediasystem.ini"));

  private SceneManager sceneManager;
  private ConnectionPool pool;
  private Framework framework;
  private DatabaseStatementTranslator translator;

  @Override
  public void start(Stage primaryStage) throws BundleException {
    System.out.println("javafx.runtime.version: " + System.getProperties().get("javafx.runtime.version"));

    Section generalSection = INI.getSection("general");
    int screenNumber = generalSection == null ? 0 : Integer.parseInt(generalSection.getDefault("screen", "0"));

    sceneManager = new DuoWindowSceneManager("MediaSystem", screenNumber);

    Section databaseIniSection = INI.getSection("database");

    ConnectionPoolDataSource dataSource = databaseIniSection == null ? new SimpleConnectionPoolDataSource("jdbc:derby:db;create=true") : configureDataSource(databaseIniSection);
    String databaseUrl = databaseIniSection == null ? "jdbc:derby:db;create=true" : databaseIniSection.get("url");

    pool = new ConnectionPool(dataSource, 5);
    translator = createDatabaseStatementTranslator(databaseUrl);

    framework = createHostedOSGiEnvironment();

    Module module = new AbstractModule() {
      private PlayerPresentation playerPresentation;
      private PluginTracker<MainMenuExtension> mainMenuExtensions = new PluginTracker<>(framework.getBundleContext(), MainMenuExtension.class);
      private EntityFactory<DatabaseObject> entityFactory = new CachingEntityFactory(framework.getBundleContext());

      @Override
      protected void configure() {
        bind(SelectMediaView.class).to(StandardView.class);
        bind(PlaybackOverlayView.class).to(PlaybackOverlayPane.class);
        bind(TaskExecutor.class).to(MessagePaneTaskExecutor.class);

        bind(new TypeLiteral<PluginTracker<MainMenuExtension>>() {}).toProvider(new Provider<PluginTracker<MainMenuExtension>>() {
          @Override
          public PluginTracker<MainMenuExtension> get() {
            return mainMenuExtensions;
          }
        });
      }

      @Provides
      public PlayerPresentation providesPlayerPresentation() {
        if(playerPresentation == null) {
          ServiceReference<PlayerFactory> serviceReference = framework.getBundleContext().getServiceReference(PlayerFactory.class);
          PlayerFactory factory = framework.getBundleContext().getService(serviceReference);

          if(factory != null) {
            try {
              playerPresentation = new PlayerPresentation(factory.create(INI));
            }
            catch(Exception e) {
              System.out.println("[SEVERE] Could not configure a Video Player: " + e);
            }
          }
          else {
            System.out.println("[SEVERE] Could not configure a Video Player, no PlayerFactory found.");
          }
        }

        return playerPresentation;
      }

      @Provides
      public SceneManager providesSceneManager() {
        return sceneManager;
      }

      @Provides
      public Ini providesIni() {
        return INI;
      }

      @Provides
      public BundleContext providesBundleContext() {
        return framework.getBundleContext();
      }

      @Provides
      public Connection providesConnection() {
        return pool.getConnection();
      }

      @Provides
      public DatabaseStatementTranslator providesDatabaseStatementTranslator() {
        return translator;
      }

      @Provides
      public EntityFactory<DatabaseObject> providesEntityFactory() {
        return entityFactory;
      }
    };

    final Injector injector = Guice.createInjector(module);

    DependencyManager dm = new DependencyManager(framework.getBundleContext());

    dm.add(dm.createComponent()
      .setInterface(SettingsStore.class.getName(), null)
      .setImplementation(injector.getInstance(SettingsStore.class))
    );

    waitFor(PlayerFactory.class);

    DatabaseUpdater updater = injector.getInstance(DatabaseUpdater.class);

    updater.updateDatabase();

    PersisterProvider.register(MediaData.class, injector.getInstance(MediaDataPersister.class));

    System.out.println("Registering components...");

    dm.add(dm.createComponent()
      .setInterface(EntityProvider.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", Identifier.class);
      }})
      .setImplementation(new IdentifierProvider())
    );

    dm.add(dm.createComponent()
      .setInterface(EntityProvider.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", MediaData.class);
      }})
      .setImplementation(new MediaDataProvider())
    );

    dm.add(dm.createComponent()
      .setInterface(EntityProvider.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", Person.class);
      }})
      .setImplementation(new PersonProvider())
    );

    dm.add(dm.createComponent()
      .setInterface(EntityFactory.class.getName(), null)
      .setImplementation(new CachingEntityFactory(framework.getBundleContext()))
    );

    dm.add(dm.createComponent()
      .setInterface(DetailPaneDecoratorFactory.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", Media.class);
      }})
      .setImplementation(new DetailPaneDecoratorFactory() {
        @Override
        public DetailPaneDecorator<?> create(DetailPane.DecoratablePane decoratablePane) {
          return new MediaDetailPaneDecorator(decoratablePane);
        }
      })
    );

    dm.add(dm.createComponent()
      .setInterface(DetailPaneDecoratorFactory.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", Person.class);
      }})
      .setImplementation(new DetailPaneDecoratorFactory() {
        @Override
        public DetailPaneDecorator<?> create(DetailPane.DecoratablePane decoratablePane) {
          return new PersonDetailPaneDecorator(decoratablePane);
        }
      })
    );

    dm.add(dm.createComponent()
      .setInterface(SelectMediaPresentationProvider.class.getName(), null)
      .setImplementation(injector.getInstance(SelectMediaPresentationProvider.class))
    );

    dm.add(dm.createComponent()
      .setInterface(PersistQueue.class.getName(), null)
      .setImplementation(injector.getInstance(PersistQueue.class))
    );

    dm.add(dm.createComponent()
      .setInterface(ItemsDao.class.getName(), null)
      .setImplementation(injector.getInstance(ItemsDao.class))
    );

    dm.add(dm.createComponent()
      .setInterface(IdentifierDao.class.getName(), null)
      .setImplementation(injector.getInstance(IdentifierDao.class))
    );

    dm.add(dm.createComponent()
      .setInterface(MediaItemConfigurator.class.getName(), null)
      .setImplementation(injector.getInstance(MediaItemConfigurator.class))
    );

    dm.add(dm.createComponent()
      .setInterface(MediaNodeCellProvider.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", Media.class);
        put("type", MediaNodeCellProvider.Type.HORIZONTAL);
      }})
      .setImplementation(new MediaNodeCellProvider() {
        @Override
        public MediaNodeCell get() {
          return new StandardCell();
        }
      })
    );

    dm.add(dm.createComponent()
      .setInterface(Setting.class.getName(), null)
      .setImplementation(new SettingGroup(framework.getBundleContext(), "video", "Video", 0))
    );

    dm.add(dm.createComponent()
      .setInterface(Setting.class.getName(), null)
      .setImplementation(new AbstractSetting("information-bar.debug-mem", 0) {
        private volatile SettingsStore settingsStore;

        @Override
        public Option createOption() {
          final BooleanProperty booleanProperty = settingsStore.getBooleanProperty("MediaSystem:InformationBar", PersistLevel.PERMANENT, "Visible");

          return new BooleanOption("Show Memory Use Information", booleanProperty, new StringBinding(booleanProperty) {
            @Override
            protected String computeValue() {
              return booleanProperty.get() ? "Yes" : "No";
            }
          });
        }
      })
      .add(dm.createServiceDependency()
        .setService(SettingsStore.class)
        .setRequired(true)
      )
    );

    System.out.println("Creating controller...");

    final ProgramController controller = injector.getInstance(ProgramController.class);

    dm.add(dm.createComponent()
      .setInterface(LocationHandler.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", MainScreenLocation.class);
      }})
      .setImplementation(new LocationHandler() {
        @Override
        public Presentation go(Location location, Presentation current) {
          return new MainScreenPresentation(controller);
        }
      })
    );

    dm.add(dm.createComponent()
      .setInterface(LocationHandler.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", PlaybackLocation.class);
      }})
      .setImplementation(new LocationHandler() {
        @Override
        public Presentation go(Location location, Presentation current) {
          return injector.getInstance(PlaybackOverlayPresentation.class);
        }
      })
    );

    dm.add(dm.createComponent()
      .setInterface(LocationHandler.class.getName(), new Hashtable<String, Object>() {{
        put("mediasystem.class", SelectMediaLocation.class);
      }})
      .setImplementation(new LocationHandler() {
        private volatile SelectMediaPresentationProvider selectMediaPresentationProvider;

        @Override
        public Presentation go(Location location, Presentation current) {
          SelectMediaPresentation presentation = current instanceof SelectMediaPresentation ? (SelectMediaPresentation)current : selectMediaPresentationProvider.get();

          presentation.setMediaTree(((SelectMediaLocation)location).getMediaRoot());

          return presentation;
        }
      })
      .add(dm.createServiceDependency()
        .setService(SelectMediaPresentationProvider.class)
        .setRequired(true)
      )
    );

    controller.showMainScreen();
  }

  @Override
  public void stop() throws InterruptedException {
    if(framework != null) {
      framework.waitForStop(5000);
    }
    if(pool != null) {
      pool.close();
    }
  }

  private void waitFor(Class<?>... classes) {
    List<Class<?>> unavailableServices = new ArrayList<>();

    for(int i = 0; i < 20; i++) {
      unavailableServices.clear();

      for(Class<?> cls : classes) {
        ServiceReference<?> serviceReference = framework.getBundleContext().getServiceReference(cls);

        if(serviceReference == null) {
          unavailableServices.add(cls);
        }
      }

      if(unavailableServices.isEmpty()) {
        return;
      }

      System.out.println("Waiting for services " + unavailableServices);

      try {
        Thread.sleep(500);
      }
      catch(InterruptedException e) {
        // Not interested
      }
    }

    System.out.println("Giving up, cannot find services " + unavailableServices);
  }

  private Framework createHostedOSGiEnvironment() throws BundleException {
    Map<String, String> config = ConfigUtil.createConfig();
    Framework framework = createFramework(config);
    framework.init();
    framework.start();

    monitorBundles(
      Paths.get("local/bundles"),
      Paths.get("../cnf/repo/com.springsource.org.apache.log4j"),
      Paths.get("../cnf/repo/jackson-core-lgpl"),
      Paths.get("../cnf/repo/jackson-mapper-lgpl"),
      Paths.get("../hs.mediasystem.ext/generated")
    );

    return framework;
  }

  private void monitorBundles(final Path... monitorPaths) {
    new Thread() {
      {
        setDaemon(true);
      }

      @Override
      public void run() {
        try {
          WatchService watchService = FileSystems.getDefault().newWatchService();

          for(Path path : monitorPaths) {
            try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(path, "*.jar")) {
              for(Path newPath : dirStream) {
                installAndStartBundle(newPath);
              }
            }

            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
          }

          for(;;) {
            WatchKey key = watchService.take();

            for(WatchEvent<?> event : key.pollEvents()) {
              Path newPath = (Path)event.context();

              if(newPath.getFileName().toString().endsWith(".jar")) {
                installAndStartBundle(newPath);
              }
            }

            key.reset();
          }
        }
        catch(IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }

      private void installAndStartBundle(Path path) {
        BundleContext bundleContext = framework.getBundleContext();

        try {
          Bundle bundle = bundleContext.installBundle(path.toUri().toString());
          bundle.start();
        }
        catch(BundleException e) {
          System.out.println("[WARN] Bundle Hot Deploy Monitor - Exception while installing bundle '" + path.toUri() + "': " + e);
          e.printStackTrace(System.out);
        }
      }

    }.start();
  }

  private static Framework createFramework(Map<String, String> config) {
    ServiceLoader<FrameworkFactory> factoryLoader = ServiceLoader.load(FrameworkFactory.class);

    for(FrameworkFactory factory : factoryLoader){
      return factory.newFramework(config);
    }

    throw new IllegalStateException("Unable to load FrameworkFactory service.");
  }

  private DatabaseStatementTranslator createDatabaseStatementTranslator(String url) {
    String databaseName = url.split(":")[1].toLowerCase();

    if(databaseName.equals("postgresql")) {
      return new SimpleDatabaseStatementTranslator(new HashMap<String, String>() {{
        put("BinaryType", "bytea");
        put("DropNotNull", "DROP NOT NULL");
        put("Sha256Type", "bytea");
        put("SerialType", "serial4");
      }});
    }

    return new SimpleDatabaseStatementTranslator(new HashMap<String, String>() {{
      put("BinaryType", "blob");
      put("DropNotNull", "NULL");
      put("Sha256Type", "char(32) for bit data");
      put("SerialType", "integer generated always as identity");
    }});
  }

  private ConnectionPoolDataSource configureDataSource(Section section)  {
    try {
      Class.forName(section.get("driverClass"));
      Properties properties = new Properties();

      for(String key : section) {
        if(!key.equals("driverClass") && !key.equals("postConnectSql") && !key.equals("url")) {
          properties.put(key, section.get(key));
        }
      }

      SimpleConnectionPoolDataSource dataSource = new SimpleConnectionPoolDataSource(section.get("url"), properties);

      dataSource.setPostConnectSql(section.get("postConnectSql"));

      return dataSource;
    }
    catch(ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private ConnectionPoolDataSource configureDataSourceAdvanced(Section section)  {
    try {
      String dataSourceClassName = section.get("dataSourceClass");
      Class<?> dataSourceClass = Class.forName(dataSourceClassName);
      Lookup lookup = MethodHandles.lookup();

      MethodHandle constructor = lookup.findConstructor(dataSourceClass, MethodType.methodType(void.class));

      @SuppressWarnings("cast")
      ConnectionPoolDataSource dataSource = (ConnectionPoolDataSource)constructor.invoke();

      for(String key : section) {
        if(!key.equals("dataSourceClass")) {
          Method setter = BeanUtils.getSetter(dataSourceClass, key);
          String value = section.get(key);
          Object parameter = value;
          Class<?> argumentType = setter.getParameterTypes()[0];

          if(argumentType == Integer.class || argumentType == int.class) {
            parameter = Integer.parseInt(value);
          }

          try {
            setter.invoke(dataSource, parameter);
          }
          catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("expected: " + argumentType, e);
          }
        }
      }

      return dataSource;
    }
    catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }
}

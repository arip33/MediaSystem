package hs.mediasystem.entity;

import hs.mediasystem.persist.Persister;
import hs.mediasystem.util.DebugConsole;
import hs.mediasystem.util.DebugConsole.CommandCallback;
import hs.subtitle.DefaultThreadFactory;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Entity<T> {
  private static final ThreadPoolExecutor PRIMARY_EXECUTOR = new ThreadPoolExecutor(5, 5, 5, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
  private static final ThreadPoolExecutor SECONDARY_EXECUTOR = new ThreadPoolExecutor(2, 2, 5, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());

  private static final Map<EnrichmentRunnable, String> PRIMARY_TASKS = new WeakHashMap<>();
  private static final Map<EnrichmentRunnable, String> SECONDARY_TASKS = new WeakHashMap<>();
  private static final Map<InstanceEnricher<?, ?>, String> ENRICHERS = new WeakHashMap<>();

  static {
    PRIMARY_EXECUTOR.setThreadFactory(new DefaultThreadFactory("EntityEnrichPrimary", Thread.NORM_PRIORITY - 2, true));
    SECONDARY_EXECUTOR.setThreadFactory(new DefaultThreadFactory("EntityEnrichSecondary", Thread.NORM_PRIORITY - 2, true));

    DebugConsole.addCommand("d", new CommandCallback() {
      @Override
      public String execute(String name, String parameters) {
        synchronized(ENRICHERS) {
          StringBuilder builder = new StringBuilder();

          builder.append("Running threads and enrichments:\n");

          builder.append("PRIMARY_EXECUTOR   : active: " + PRIMARY_EXECUTOR.getActiveCount() + "/" + PRIMARY_EXECUTOR.getPoolSize() + "   queued: " + PRIMARY_EXECUTOR.getQueue().size() + "\n");

          for(EnrichmentRunnable runnable : PRIMARY_TASKS.keySet()) {
            builder.append("- ");
            builder.append(runnable);
            builder.append("\n");
          }

          builder.append("SECONDARY_EXECUTOR : active: " + SECONDARY_EXECUTOR.getActiveCount() + "/" + SECONDARY_EXECUTOR.getPoolSize() + "   queued: " + SECONDARY_EXECUTOR.getQueue().size() + "\n");

          for(EnrichmentRunnable runnable : SECONDARY_TASKS.keySet()) {
            builder.append("- ");
            builder.append(runnable);
            builder.append("\n");
          }

          builder.append("\n");

          for(InstanceEnricher<?, ?> enricher : ENRICHERS.keySet()) {
            String text = enricher.toString();

            if(!text.startsWith("INACTIVE")) {
              builder.append(text + "\n");
            }
          }

          return builder.toString();
        }
      }
    });
  }

  static void submit(boolean primary, EnrichmentRunnable runnable) {
    synchronized(ENRICHERS) {
      Executor executor = primary ? PRIMARY_EXECUTOR : SECONDARY_EXECUTOR;
      Map<EnrichmentRunnable, String> tasks = primary ? PRIMARY_TASKS : SECONDARY_TASKS;

      tasks.put(runnable, "");
      executor.execute(runnable);
    }
  }

  private static void registerEnricher(InstanceEnricher<?, ?> enricher) {
    synchronized(ENRICHERS) {
      ENRICHERS.put(enricher, "");
    }
  }

  private EntityFactory<Object> factory;
  private Persister<T> persister;

  @SuppressWarnings("unchecked")
  public void setEntityFactory(EntityFactory<?> factory) {
    this.factory = (EntityFactory<Object>)factory;
  }

  public <C extends Entity<?>> C create(Class<C> cls, Object key) {
    if(factory == null) {
      throw new RuntimeException("No EntityFactory set for: " + this);
    }

    return factory.create(cls, key);
  }

  public <K> K getKey() {
    if(factory == null) {
      throw new RuntimeException("No EntityFactory set for: " + this);
    }

    return factory.getAssociatedKey(this);
  }

  public void setPersister(Persister<T> persister) {
    this.persister = persister;
  }

  public static class EnrichTask<T, R> extends EnrichmentRunnable {
    public EnrichTask(final T parent, final InstanceEnricher<T, R> enricher) {
      super(0, new Runnable() {
        @Override
        public void run() {
          final R result = enricher.enrich(parent);

          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              enricher.update(parent, result);
            }
          });
        }
      });

      Entity.registerEnricher(enricher);
    }
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T)this;
  }

  protected <P> SimpleEntityProperty<P> entity(String name) {
    return new SimpleEntityProperty<P>(this, name) {
      private boolean enricherCalled;

      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }

      @Override
      public P get() {
        P value = super.get();

        if(value == null && getEnricher() != null && !enricherCalled) {
          enricherCalled = true;

          submit(true, new EnrichTask<>(self(), getEnricher()));
        }

        return value;
      }
    };
  }

  protected <P> ObjectProperty<P> object(String name) {
    return new SimpleObjectProperty<P>(this, name) {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected StringProperty stringProperty(String initialValue) {
    return new SimpleStringProperty(initialValue) {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected StringProperty stringProperty() {
    return stringProperty(null);
  }

  protected IntegerProperty integerProperty() {
    return new SimpleIntegerProperty() {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected LongProperty longProperty() {
    return new SimpleLongProperty() {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected BooleanProperty booleanProperty() {
    return new SimpleBooleanProperty() {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected FloatProperty floatProperty() {
    return new SimpleFloatProperty() {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }

  protected DoubleProperty doubleProperty() {
    return new SimpleDoubleProperty() {
      @Override
      protected void invalidated() {
        if(persister != null) {
          persister.queueAsDirty(self());
          get();
        }
      }
    };
  }
}

package hs.mediasystem.framework;

import hs.mediasystem.Controller;
import hs.ui.controls.AbstractGroup;

public class Screen {
  private final AbstractBlock<?> block;
  private final Extensions extensions;

  public Screen(AbstractBlock<?> block, Extensions extensions) {
    this.block = block;
    this.extensions = extensions;
  }

  public Screen(AbstractBlock<?> block) {
    this(block, new Extensions());
  }

  public AbstractGroup<?> getContent(Controller controller) {
    return block.getContent(controller, extensions);
  }
  
  public State getState() {
    return block.getState(extensions);
  }
  
  public void applyConfig(Config<?> config) {
    if(block.getClass().equals(config.type())) {
      block.applyConfigWithCast(config);
    }
    
    for(Screen screen : extensions) {
      screen.applyConfig(config);
    }
  }
}
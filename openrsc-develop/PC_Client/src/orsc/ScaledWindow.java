package orsc;

import orsc.util.Utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.*;

import static orsc.OpenRSC.applet;
import static orsc.OpenRSC.jframe;

   
                                                                                               
                                                                                  
                                                                                   
      
                                                                             
   
public class ScaledWindow extends JFrame implements WindowListener, FocusListener, ComponentListener,
	MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

	private static ScaledWindow instance = null;
	private static boolean initialRender = true;
	private static int javaVersion = 0;
	private static int numCores;
	private static boolean isMacOS = false;
	private static boolean shouldRealign = false;
	private int frameWidth = 0;
	private int frameHeight = 0;
	private ScaledViewport scaledViewport;
	private int viewportWidth = 0;
	private int viewportHeight = 0;
	private BufferedImage unscaledBackground = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	private int previousUnscaledWidth;
	private int previousUnscaledHeight;

	private static final float MAX_INTEGER_SCALE = 6.0f;
	private static final float MAX_INTERPOLATION_SCALE = 4.0f;

	                                                     
	private ScaledWindow() {
		try {
			                                
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			System.out.println("Unable to set L&F: Unsupported look and feel");
		} catch (ClassNotFoundException e) {
			System.out.println("Unable to set L&F: Class not found");
		} catch (InstantiationException e) {
			System.out.println("Unable to set L&F: Class object cannot be instantiated");
		} catch (IllegalAccessException e) {
			System.out.println("Unable to set L&F: Illegal access exception");
		}

		System.out.println("Creating scaled window");

		                                           
		try {
			SwingUtilities.invokeAndWait(() -> {
				javaVersion = Utils.getJavaVersion();
				numCores = Runtime.getRuntime().availableProcessors();

				runInit();
			});
		} catch (InvocationTargetException e) {
			System.out.println("There was a thread-related error while setting up the scaled window!");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println(
				"There was a thread-related error while setting up the scaled window! The window may not be initialized properly!");
			e.printStackTrace();
		}
	}

	private void runInit() {
		                        
		setBackground(Color.black);
		setFocusTraversalKeysEnabled(false);

		                       
		addWindowListener(this);
		addComponentListener(this);
		addFocusListener(this);
		addKeyListener(this);

		                                              
		isMacOS = Utils.isMacOS();

		if (isMacOS) {
			try {
				Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
				Class params[] = new Class[] {Window.class, Boolean.TYPE};
				@SuppressWarnings("unchecked")
				Method method = util.getMethod("setWindowCanFullScreen", params);
				method.invoke(util, this, true);
			} catch (Exception ignored) {
			}
		}

		                                  
		setMinimumSize(new Dimension(512, 346));

		                                         
		setIconImage(Utils.getImage("icon.png").getImage());

		                         
		scaledViewport = new ScaledViewport();

		scaledViewport.addMouseListener(this);
		scaledViewport.addMouseMotionListener(this);
		scaledViewport.addMouseWheelListener(this);

		scaledViewport.setSize(getSize());
		scaledViewport.setBackground(Color.black);
		scaledViewport.revalidate();
		scaledViewport.repaint();
		scaledViewport.setVisible(true);

		add(scaledViewport);

		pack();
		revalidate();
		repaint();

		                                                              
		Dimension maxEffectiveWindowSize = getMaximumEffectiveWindowSize();
		int maxRenderingScalar = 1;
		for (int i = 6; i >= 1; i--) {
			float width = 512 * i;
			float height = 346 * i;

			if (width <= maxEffectiveWindowSize.width && height <= maxEffectiveWindowSize.height) {
				maxRenderingScalar = i + 1;
				break;
			}
		}

		List<Float> integerScalars = new ArrayList<>();
		for (float i = 1.0f; i <= maxRenderingScalar && i <= MAX_INTEGER_SCALE; i++) {
			integerScalars.add(i);
		}

		mudclient.integerScalars = integerScalars;

		List<Float> interpolationScalars = new ArrayList<>();
		for (float i = 1.0f; i <= maxRenderingScalar && i <= MAX_INTERPOLATION_SCALE; i += 0.5f) {
			interpolationScalars.add(i);
		}

		mudclient.interpolationScalars = interpolationScalars;
	}

	   
                                                                                                 
                                                                              
   
                                                                               
    
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);

		frameWidth = width;
		frameHeight = height;
	}

	                                                                
	public void setWindowRealignmentIntent(boolean flag) {
		shouldRealign = flag;
	}

	   
                                                                                                  
                    
    
	private void alignWindow() {
		Rectangle currentScreenBounds = getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();

		int x = ((currentScreenBounds.width - frameWidth) / 2) + currentScreenBounds.x;
		int y = ((currentScreenBounds.height - frameHeight) / 2) + currentScreenBounds.y;

		                          
		setLocation(x, y);
	}

	   
                                                                                               
                  
    
	public Dimension getMaximumEffectiveWindowSize() {
		Dimension maximumWindowSize = getMaximumWindowSize();

		           
		int windowWidth = maximumWindowSize.width - getWindowWidthInsets();
		int windowHeight = maximumWindowSize.height - getWindowHeightInsets();

		if (Utils.isModernWindowsOS()) {
			windowWidth += 16;
			windowHeight += 8;
		}

		return new Dimension(windowWidth, windowHeight);
	}

	                                                       
	public Dimension getMaximumWindowSize() {
		GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration().getDevice().getDefaultConfiguration();
		Rectangle screenBounds = graphicsConfiguration.getBounds();
		Insets screenInsets = getToolkit().getScreenInsets(graphicsConfiguration);

		                                                                             
		int maxWidth = screenBounds.width - screenInsets.left - screenInsets.right;
		int maxHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

		return new Dimension(maxWidth, maxHeight);
	}

	                       
	public void launchScaledWindow() {
		setLocationRelativeTo(null);
		setVisible(true);
	}

	   
                                                                  
                                  
    
	public void setGameImage(BufferedImage gameImage) {
		if (gameImage == null) {
			return;
		}

		if (scaledViewport.isViewportImageLoaded()) {
			if (initialRender) {
				                                                                       
				resizeWindowToScalar();
				initialRender = false;
			}

			viewportWidth = gameImage.getWidth();
			viewportHeight = gameImage.getHeight();
		}

		if (mudclient.renderingScalar == 1.0f) {
			                           
			int newUnscaledWidth = gameImage.getWidth();
			int newUnscaledHeight = gameImage.getHeight();

			if (previousUnscaledWidth != newUnscaledWidth || previousUnscaledHeight != newUnscaledHeight) {
				unscaledBackground = new BufferedImage(newUnscaledWidth, newUnscaledHeight, gameImage.getType());
				previousUnscaledWidth = newUnscaledWidth;
				previousUnscaledHeight = newUnscaledHeight;
			}

			                                                      
			Graphics2D g2d = (Graphics2D) unscaledBackground.getGraphics();
			g2d.drawImage(gameImage, 0, 0, null);
			g2d.dispose();

			scaledViewport.setViewportImage(unscaledBackground);

			scaledViewport.repaint();
		} else {
			                         
			scaledViewport.setViewportImage(gameImage);

			int scaledWidth = Math.round(viewportWidth * mudclient.renderingScalar);
			int scaledHeight = Math.round(viewportHeight * mudclient.renderingScalar);

			try {
				SwingUtilities.invokeAndWait(() -> scaledViewport.paintImmediately(0, 0, scaledWidth, scaledHeight));
			} catch (InterruptedException | InvocationTargetException ignored) {
				        
			}
		}
	}

	public boolean isViewportLoaded() {
		return scaledViewport.isViewportImageLoaded();
	}

	public int getWindowWidthInsets() {
		return getInsets().left + getInsets().right;
	}

	public int getWindowHeightInsets() {
		return getInsets().top + getInsets().bottom;
	}

	                                             
	public void resizeWindowToScalar() {
		Dimension minimumWindowSizeForScalar = getMinimumWindowSizeForScalar();

		if (!getSize().equals(minimumWindowSizeForScalar)) {
			                                                          
			                                                      
			setWindowRealignmentIntent(true);

			setMinimumSize(minimumWindowSizeForScalar);
			setSize(minimumWindowSizeForScalar);
		} else {
			                                                                     
			                                                                  
			                                                     
			setMinimumSize(minimumWindowSizeForScalar);
			resizeApplet();
		}
	}

	                                                                           
	private Dimension getMinimumWindowSizeForScalar() {
		Dimension minimumViewPortSizeForScalar = getMinimumViewportSizeForScalar();

		int frameWidth = minimumViewPortSizeForScalar.width + getWindowWidthInsets();
		int frameHeight = minimumViewPortSizeForScalar.height + getWindowHeightInsets();

		return new Dimension(frameWidth, frameHeight);
	}

	                                                                            
	public Dimension getMinimumViewportSizeForScalar() {
		return new Dimension(Math.round(512 * mudclient.renderingScalar), Math.round(346 * mudclient.renderingScalar));
	}

	                                                          
	private void resizeApplet() {
		if (mudclient.renderingScalar == 0.0f || !isViewportLoaded()) {
			return;
		}

		int newWidth = Math.round(scaledViewport.getWidth() / mudclient.renderingScalar);
		int newHeight = Math.round(scaledViewport.getHeight() / mudclient.renderingScalar);

		if (applet != null) {
			applet.setSize(newWidth, newHeight);
			applet.resizeMudclient(newWidth, newHeight);
		}

		if (shouldRealign) {
			setWindowRealignmentIntent(false);
			alignWindow();
		}
	}

	                                                                                 
	public void validateAppletSize() {
		if (applet == null) return;

		int newWidth = Math.round(scaledViewport.getWidth() / mudclient.renderingScalar);
		int newHeight = Math.round(scaledViewport.getHeight() / mudclient.renderingScalar);

		if (applet.getWidth() != newWidth || applet.getHeight() != newHeight) {
			applet.setSize(newWidth, newHeight);
			applet.resizeMudclient(newWidth, newHeight);
		}
	}

	  
                                                 
    

	@Override
	public void windowClosed(WindowEvent e) {
		jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSED));
	}

	@Override
	public void windowClosing(WindowEvent e) {
		jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSING));
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	  
                                                
    

	@Override
	public void focusGained(FocusEvent e) {}

	@Override
	public void focusLost(FocusEvent e) {
		if (applet.getKeyHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.resetArrowKeys();
	}

	  
                             
    

	@Override
	public void componentResized(ComponentEvent e) {
		resizeApplet();

		frameWidth = e.getComponent().getWidth();
		frameHeight = e.getComponent().getHeight();
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}

	  
                                                                      
                                     
    

	@Override
	public void mouseClicked(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseClicked(mapMouseEvent(e));
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mousePressed(mapMouseEvent(e));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseReleased(mapMouseEvent(e));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseEntered(mapMouseEvent(e));
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseExited(mapMouseEvent(e));
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseDragged(mapMouseEvent(e));
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseMoved(mapMouseEvent(e));
	}

	private static MouseEvent mapMouseEvent(MouseEvent e) {
		Component mouseEventSource = (Component) e.getSource();
		int mouseEventId = e.getID();
		long mouseEventWhen = e.getWhen();
		int mouseEventModifiers = e.getModifiers();
		int mappedMouseEventX = Math.round(e.getX() / mudclient.renderingScalar);
		int mappedMouseEventY = Math.round(e.getY() / mudclient.renderingScalar);
		int mouseEventXOnScreen = e.getXOnScreen();
		int mouseEventYOnScreen = e.getYOnScreen();
		int mouseEventClickCount = e.getClickCount();
		boolean mouseEventPopupTrigger = e.isPopupTrigger();
		int mouseEventButton = e.getButton();

		return new MouseEvent(
			mouseEventSource,
			mouseEventId,
			mouseEventWhen,
			mouseEventModifiers,
			mappedMouseEventX,
			mappedMouseEventY,
			mouseEventXOnScreen,
			mouseEventYOnScreen,
			mouseEventClickCount,
			mouseEventPopupTrigger,
			mouseEventButton);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getMouseHandler().mouseWheelMoved(mapMouseWheelEvent(e));
	}

	private static MouseWheelEvent mapMouseWheelEvent(MouseWheelEvent e) {
		Component mouseWheelEventSource = (Component) e.getSource();
		int mouseWheelEventId = e.getID();
		long mouseWheelEventWhen = e.getWhen();
		int mouseWheelEventModifiers = e.getModifiers();
		int mappedMouseWheelEventX = Math.round(e.getX() / mudclient.renderingScalar);
		int mappedMouseWheelEventY = Math.round(e.getY() / mudclient.renderingScalar);
		int mouseWheelEventXOnScreen = e.getXOnScreen();
		int mouseWheelEventYOnScreen = e.getYOnScreen();
		int mouseWheelEventClickCount = e.getClickCount();
		boolean mouseWheelEventPopupTrigger = e.isPopupTrigger();
		int mouseWheelEventScrollType = e.getScrollType();
		int mouseWheelEventScrollAmount = e.getScrollAmount();
		int mouseWheelEventWheelRotation = e.getWheelRotation();
		double mouseWheelEventPreciseWheelRotation = e.getPreciseWheelRotation();

		return new MouseWheelEvent(
			mouseWheelEventSource,
			mouseWheelEventId,
			mouseWheelEventWhen,
			mouseWheelEventModifiers,
			mappedMouseWheelEventX,
			mappedMouseWheelEventY,
			mouseWheelEventXOnScreen,
			mouseWheelEventYOnScreen,
			mouseWheelEventClickCount,
			mouseWheelEventPopupTrigger,
			mouseWheelEventScrollType,
			mouseWheelEventScrollAmount,
			mouseWheelEventWheelRotation,
			mouseWheelEventPreciseWheelRotation);
	}

	  
                                                            
    

	@Override
	public void keyTyped(KeyEvent e) {
		if (applet.getKeyHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getKeyHandler().keyTyped(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getKeyHandler().keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (applet.getMouseHandler() == null || mudclient.renderingScalar == 0.0f) return;

		applet.getKeyHandler().keyReleased(e);
	}

	   
                                                         
    
	public enum ScalingAlgorithm {
		INTEGER_SCALING,
		BILINEAR_INTERPOLATION,
		BICUBIC_INTERPOLATION
	}

	   
                                                                                        
    
	public static int getBufferedImageType() {
		if (mudclient.scalingType == ScalingAlgorithm.INTEGER_SCALING) {
			return BufferedImage.TYPE_INT_RGB;
		} else if (mudclient.scalingType == ScalingAlgorithm.BILINEAR_INTERPOLATION) {
			return BufferedImage.TYPE_3BYTE_BGR;
		} else if (mudclient.scalingType == ScalingAlgorithm.BICUBIC_INTERPOLATION) {
			return BufferedImage.TYPE_3BYTE_BGR;
		}

		return BufferedImage.TYPE_INT_RGB;
	}

	   
                                                                                            
    
	public static int getAffineTransformOpType() {
		if (mudclient.scalingType == ScalingAlgorithm.INTEGER_SCALING) {
			return AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
		} else if (mudclient.scalingType == ScalingAlgorithm.BILINEAR_INTERPOLATION) {
			return AffineTransformOp.TYPE_BILINEAR;
		} else if (mudclient.scalingType == ScalingAlgorithm.BICUBIC_INTERPOLATION) {
			return AffineTransformOp.TYPE_BICUBIC;
		}

		return -1;
	}

	   
                                                                       
   
                                      
    
	public static ScaledWindow getInstance() {
		if (instance == null) {
			synchronized (ScaledWindow.class) {
				instance = new ScaledWindow();
			}
		}
		return instance;
	}

	  
                   
    

	                                                                             
	private static class ScaledViewport extends JPanel {
		BufferedImage interpolationBackground = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
		BufferedImage viewportImage;

		int previousWidth = 0;
		int previousHeight = 0;

		int newWidth;
		int newHeight;

		public ScaledViewport() {
			super();
			setOpaque(true);
			setBackground(Color.black);
		}

		                                              
		public void setViewportImage(BufferedImage gameImage) {
			viewportImage = gameImage;
		}

		                                              
		public boolean isViewportImageLoaded() {
			return viewportImage != null;
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (viewportImage == null
				|| getInstance().viewportWidth == 0
				|| getInstance().viewportHeight == 0) {
				return;
			}

			                                                         
			if (mudclient.renderingScalar == 1.0f) {
				g.drawImage(viewportImage, 0, 0, null);
				return;
			}

			newWidth = Math.round(viewportImage.getWidth() * mudclient.renderingScalar);
			newHeight = Math.round(viewportImage.getHeight() * mudclient.renderingScalar);

			                                                                                    
			                                                                         
			if (mudclient.scalingType == ScalingAlgorithm.INTEGER_SCALING) {
				                                                                                     
				if (isMacOS && javaVersion >= 19) {
					g.setClip(0, 0, newWidth, newHeight);
				}

				g.drawImage(viewportImage, 0, 0, newWidth, newHeight, null);
			} else {
				if (interpolationBackground == null) {
					return;
				}

				                                                                 
				if (previousWidth != newWidth || previousHeight != newHeight) {
					interpolationBackground = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);

					previousWidth = newWidth;
					previousHeight = newHeight;
				}

				Graphics2D g2d = (Graphics2D) interpolationBackground.getGraphics();

				                                                        
				                                            
				if (numCores > 4) {
					g2d.drawImage(multiThreadedInterpolationScaling(viewportImage, newWidth, newHeight), 0, 0, null);
				} else {
					g2d.drawImage(affineTransformScale(viewportImage, newWidth, newHeight), 0, 0, null);
				}

				g2d.dispose();

				                                      
				g.drawImage(interpolationBackground, 0, 0, null);
			}
		}

		                                                                                        
		private BufferedImage multiThreadedInterpolationScaling(
			BufferedImage originalImage, int width, int height) {
			BufferedImage[] splitImages = splitImage(originalImage);

			CompletableFuture<BufferedImage> future0 =
				CompletableFuture.supplyAsync(() -> interpolationScale(splitImages[0], width, height, 0));
			CompletableFuture<BufferedImage> future1 =
				CompletableFuture.supplyAsync(() -> interpolationScale(splitImages[1], width, height, 1));
			CompletableFuture<BufferedImage> future2 =
				CompletableFuture.supplyAsync(() -> interpolationScale(splitImages[2], width, height, 2));
			CompletableFuture<BufferedImage> future3 =
				CompletableFuture.supplyAsync(() -> interpolationScale(splitImages[3], width, height, 3));

			List<BufferedImage> scaledImages =
				Stream.of(future0, future1, future2, future3)
					.map(CompletableFuture::join)
					.collect(Collectors.toList());

			return stitchImageParts(scaledImages);
		}

		   
                                                                                              
                              
     
		private static BufferedImage[] splitImage(BufferedImage originalImage) {
			int offsetBuffer = 10;

			BufferedImage[] imageParts = new BufferedImage[4];

			int widthPadding = (originalImage.getWidth() & 1) == 1 ? 1 : 0;
			int heightPadding = (originalImage.getHeight() & 1) == 1 ? 1 : 0;

			originalImage = padImageIfNeeded(originalImage, widthPadding, heightPadding);

			int halfWidth = originalImage.getWidth() / 2;
			int halfHeight = originalImage.getHeight() / 2;

			imageParts[0] = originalImage.getSubimage(0, 0, halfWidth + offsetBuffer, halfHeight + offsetBuffer);
			imageParts[1] = originalImage.getSubimage(halfWidth - offsetBuffer, 0, halfWidth + offsetBuffer, halfHeight + offsetBuffer);
			imageParts[2] = originalImage.getSubimage(0, halfHeight - offsetBuffer, halfWidth + offsetBuffer, halfHeight + offsetBuffer);
			imageParts[3] = originalImage.getSubimage(halfWidth - offsetBuffer, halfHeight - offsetBuffer, halfWidth + offsetBuffer, halfHeight + offsetBuffer);

			return imageParts;
		}

		                                                                                    
		private static BufferedImage padImageIfNeeded(
			BufferedImage originalImage, int widthPadding, int heightPadding) {
			if (widthPadding == 0 && heightPadding == 0) {
				return originalImage;
			}

			BufferedImage paddedImage =
				new BufferedImage(
					originalImage.getWidth() + widthPadding,
					originalImage.getHeight() + heightPadding,
					originalImage.getType());

			Graphics2D g2d = (Graphics2D) paddedImage.getGraphics();
			g2d.drawImage(originalImage, 0, 0, null);
			g2d.dispose();

			return paddedImage;
		}

		   
                                                                                                
                   
     
		private static BufferedImage interpolationScale(
			BufferedImage originalImage, int width, int height, int n) {
			int scaledOffsetBuffer = (int) (10 * mudclient.renderingScalar);

			BufferedImage scaledImage =
				affineTransformScale(originalImage, (width / 2) + scaledOffsetBuffer, (height / 2) + scaledOffsetBuffer);

			if (n == 0) {
				return scaledImage.getSubimage(
					0,
					0,
					scaledImage.getWidth() - scaledOffsetBuffer,
					scaledImage.getHeight() - scaledOffsetBuffer);
			} else if (n == 1) {
				return scaledImage.getSubimage(
					scaledOffsetBuffer,
					0,
					scaledImage.getWidth() - scaledOffsetBuffer,
					scaledImage.getHeight() - scaledOffsetBuffer);
			} else if (n == 2) {
				return scaledImage.getSubimage(
					0,
					scaledOffsetBuffer,
					scaledImage.getWidth() - scaledOffsetBuffer,
					scaledImage.getHeight() - scaledOffsetBuffer);
			} else {
				return scaledImage.getSubimage(
					scaledOffsetBuffer,
					scaledOffsetBuffer,
					scaledImage.getWidth() - scaledOffsetBuffer,
					scaledImage.getHeight() - scaledOffsetBuffer);
			}
		}

		                                                                              
		public static BufferedImage affineTransformScale(
			BufferedImage originalImage, int width, int height) {
			int imageWidth = originalImage.getWidth();
			int imageHeight = originalImage.getHeight();

			double scaleX = (double) width / imageWidth;
			double scaleY = (double) height / imageHeight;

			AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
			AffineTransformOp scalingOp = new AffineTransformOp(scaleTransform, getAffineTransformOpType());

			return scalingOp.filter(originalImage, new BufferedImage(width, height, originalImage.getType()));
		}

		                                                               
		private static BufferedImage stitchImageParts(List<BufferedImage> imageParts) {
			int maxHeight = 0;
			int maxWidth = 0;

			for (BufferedImage imagePart : imageParts) {
				int imageWidth = imagePart.getWidth(null);
				int imageHeight = imagePart.getHeight(null);

				maxHeight = Math.max(maxHeight, imageHeight);
				maxWidth = Math.max(maxWidth, imageWidth);
			}

			BufferedImage canvas = new BufferedImage(maxWidth * 2, maxHeight * 2, BufferedImage.TYPE_3BYTE_BGR);
			Graphics g = canvas.getGraphics();

			g.setColor(Color.black);
			g.fillRect(0, 0, canvas.getWidth(null), canvas.getHeight(null));

			int currCol = 0;
			int currRow = 0;

			for (BufferedImage imagePart : imageParts) {
				g.drawImage(imagePart, currCol * maxWidth, currRow * maxHeight, null);
				currCol++;

				if (currCol >= 2) {
					currCol = 0;
					currRow++;
				}
			}

			return canvas;
		}
	}
}

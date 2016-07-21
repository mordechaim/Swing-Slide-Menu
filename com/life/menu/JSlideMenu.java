package com.life.menu;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.util.LinkedList;

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InternalFrameUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.plaf.metal.MetalInternalFrameTitlePane;
import javax.swing.plaf.metal.MetalInternalFrameUI;
import javax.swing.plaf.synth.SynthInternalFrameUI;

public class JSlideMenu {

	private float listCoveringPercent;
	private int minListWidth;
	private boolean hideList;
	private boolean showing;
	// private boolean closeOnIconify;
	private boolean closeOnOffMenuTouch;///////////////////////////////////////////////////////////////////////////////// TODO

	private JInternalFrame window;
	private JScrollPane scroll;
	private JPanel itemPanel;
	private JSlideMenuItem activeItem;
	private JList<JSlideMenuItem> list;
	private DefaultListModel<JSlideMenuItem> model;

	private Timer listIncrementor;
	private Timer listDecrementor;
	private Timer itemIncrementor;
	private Timer itemDecrementor;
	private float listDelta;
	private float itemDelta;

	private Component comp;

	private LinkedList<SlideMenuListener> listeners;

	public JSlideMenu(JFrame parentFrame, Component comp) {
		this.comp = comp;

		listCoveringPercent = 0.25f;
		minListWidth = 100;
		hideList = false;

		listDelta = 0f;
		itemDelta = 1f;
		listIncrementor = new Timer(10, new DeltaController(true, true));
		listDecrementor = new Timer(10, new DeltaController(false, true));
		itemIncrementor = new Timer(10, new DeltaController(true, false));
		itemDecrementor = new Timer(10, new DeltaController(false, false));

		model = new DefaultListModel<>();
		list = new JList<JSlideMenuItem>(model);
		scroll = new JScrollPane(list) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				if (hideList)
					return new Dimension((int) ((double) comp.getWidth() * listDelta), comp.getHeight());

				return new Dimension( // Make sure it won't stick out of frame,
										// if minListWidth > getWidth
						Math.min(
								(int) ((double) comp.getWidth() * listDelta), Math
										.max((int) ((double) minListWidth * listDelta),
												(int) ((double) (comp.getWidth()
														/ (100 / (int) (listCoveringPercent * 100)) * listDelta)))),
						comp.getHeight());
			}

		};

		scroll.setBorder(null);
		scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(evt -> {
			if (evt.getValueIsAdjusting() || list.getSelectedIndex() < 0)
				return;

			openItem(getItem(list.getSelectedIndex()));
		});

		itemPanel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				if (hideList)
					return new Dimension((int) ((double) comp.getWidth() * itemDelta), comp.getHeight());

				return new Dimension((int) ((double) (comp.getWidth() - (list.getWidth() * listDelta)) * itemDelta),
						comp.getHeight());

			}

		};

		itemPanel.setOpaque(false);

		itemPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (activeItem == null)
					close();
			}
		});

		window = new JInternalFrame();
		window.setLayout(new BorderLayout());
		window.add(scroll, BorderLayout.WEST);
		window.add(itemPanel, BorderLayout.CENTER);
		((BasicInternalFrameUI) window.getUI()).setNorthPane(null);
		window.setBorder(null);

		parentFrame.getLayeredPane().add(window, Integer.valueOf(100));

		// shadow
		window.setOpaque(false);
		window.getContentPane().setBackground(new Color(0, 0, 0, 115));

		parentFrame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent evt) {
				list.invalidate();
				window.pack();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				window.setLocation(comp.getLocation());
			}
		});

		parentFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				close();
			}
		});

		listeners = new LinkedList<>();
	}

	public void addItem(JSlideMenuItem item) {
		model.addElement(item);
	}

	public void addItem(int index, JSlideMenuItem item) {
		model.add(index, item);
	}

	public void removeItem(int index) {
		model.remove(index);
	}

	public void removeItem(JSlideMenuItem item) {
		model.removeElement(item);
	}

	public int indexOf(JSlideMenuItem item) {
		return model.indexOf(item);
	}

	public JSlideMenuItem getItem(int index) {
		return model.get(index);
	}

	public void open() {
		if (showing)
			return;

		showing = true;
		window.setLocation(comp.getLocation());
		window.setVisible(true);

		listDecrementor.stop();
		listIncrementor.start();

		for (SlideMenuListener l : listeners)
			l.menuOpened(this);
	}

	public void close() {
		if (!showing)
			return;

		showing = false;
		if (activeItem != null)
			closeItem();

		list.getSelectionModel().clearSelection();
		listIncrementor.stop();
		listDecrementor.start();

		for (SlideMenuListener l : listeners)
			l.menuClosed(this);
	}

	public void openItem(int index) {
		openItem(model.get(index));
	}

	public void openItem(JSlideMenuItem item) {
		if (hideList) {
			listIncrementor.stop();
			listDecrementor.start();
		}

		itemPanel.removeAll();
		item.setup(itemPanel);

		boolean hasActiveItem = activeItem != null;
		activeItem = item;

		itemPanel.setOpaque(true);

		if (!hasActiveItem) {
			itemDelta = 0f;
			itemDecrementor.stop();
			itemIncrementor.start();
		}
		window.pack();

		for (SlideMenuListener l : listeners)
			l.itemOpened(item);
	}

	public void closeItem() {
		JSlideMenuItem i = activeItem;
		activeItem = null;
		itemIncrementor.stop();
		itemDecrementor.start();
		if (hideList) {
			listDecrementor.stop();
			listIncrementor.start();
		}
		list.getSelectionModel().clearSelection();
		window.pack();

		for (SlideMenuListener l : listeners)
			l.itemClosed(i);
	}

	public void setHideList(boolean aFlag) {
		if (hideList == aFlag)
			return;

		hideList = aFlag;
		if (activeItem != null)
			list.setVisible(!aFlag);

		list.invalidate();
		window.pack();
	}

	public void setListCoveringPercent(float percent) {
		if (percent == listCoveringPercent)
			return;

		listCoveringPercent = percent;
		list.invalidate();
		window.pack();
	}

	public boolean getHideList() {
		return hideList;
	}

	public JSlideMenuItem activeItem() {
		return activeItem;
	}

	public float getListCoveringPercent() {
		return listCoveringPercent;
	}

	public int getMinListWidth() {
		return minListWidth;
	}

	public void setMinListWidth(int lw) {
		minListWidth = lw;
		list.invalidate();
		window.pack();
	}

	public JList<JSlideMenuItem> getList() {
		return list;
	}

	public JScrollPane getScrollPane() {
		return scroll;
	}

	public void addSlideMenuListener(SlideMenuListener l) {
		listeners.add(l);
	}

	public void removeSlideMenuListener(SlideMenuListener l) {
		listeners.remove(l);
	}

	public boolean isShowing() {
		return showing;
	}

	private class DeltaController implements ActionListener {

		final static float deltaStep = .1f;
		final static int size = 20;
		boolean increment;
		boolean listController;

		DeltaController(boolean increment, boolean list) {
			this.increment = increment;
			listController = list;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

			if (increment) {
				if (listController) {
					if (listDelta + deltaStep >= 1f) {
						listDelta = 1f;
						((Timer) evt.getSource()).stop();
					} else {
						listDelta += deltaStep;
					}
				} else {
					if (itemDelta + deltaStep >= 1f) {
						itemDelta = 1f;
						((Timer) evt.getSource()).stop();
					} else {
						itemDelta += deltaStep;
					}
				}

			} else {
				if (listController) {
					if (listDelta - deltaStep <= 0f) {
						listDelta = 0f;
						((Timer) evt.getSource()).stop();
						if (!isShowing())
							window.setVisible(false);
					} else {
						listDelta -= deltaStep;
					}
				} else {
					if (itemDelta - deltaStep <= 0f) {
						itemDelta = 1f;
						((Timer) evt.getSource()).stop();
						itemPanel.removeAll();
						itemPanel.setOpaque(false);
					} else {
						itemDelta -= deltaStep;
					}
				}
			}

			list.invalidate();
			window.pack();
		}

	}

}
package eu.lestard.snakefx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import eu.lestard.snakefx.util.Function;
import eu.lestard.snakefx.viewmodel.ViewModel;

/**
 * This class is the game loop of the game.
 * 
 * @author manuel.mauky
 * 
 */
public class GameLoop {


	private static final int ONE_SECOND = 1000;

	private Timeline timeline;

	private final List<Function> actions = new ArrayList<>();

	private final ViewModel viewModel;

	public GameLoop(ViewModel viewModel) {
		this.viewModel = viewModel;
		viewModel.collisionProperty().addListener(new CollisionListener());
		viewModel.speedProperty().addListener(new SpeedChangeListener());
		viewModel.gameloopStatusProperty().addListener(new StatusChangedListener());

		init();
	}


	/**
	 * Added Actions are called on every keyframe of the GameLoop. The order of
	 * invocation is not garanteed.
	 * 
	 * @param actions
	 *            the action that gets called.
	 */
	public void addActions(Function... actions) {
		this.actions.addAll(Arrays.asList(actions));
	}

	/**
	 * Initialize the timeline instance.
	 */
	private void init() {
		timeline = TimelineBuilder.create().cycleCount(Animation.INDEFINITE).keyFrames(buildKeyFrame()).build();

		// in this place we can't use a direct binding as the ViewModel property
		// is can also be changed in other places.
		timeline.statusProperty().addListener(new ChangeListener<Status>() {
			@Override
			public void changed(ObservableValue<? extends Status> arg0, Status arg1, Status newValue) {
				viewModel.gameloopStatusProperty().set(newValue);
			}
		});
	}

	/**
	 * This method creates a {@link KeyFrame} instance according to the
	 * configured framerate.
	 */
	private KeyFrame buildKeyFrame() {

		Duration duration = Duration.millis(ONE_SECOND / viewModel.speedProperty().get().getFps());

		KeyFrame frame = new KeyFrame(duration, new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent arg0) {
				for (Function action : actions) {
					action.call();
				}
			}
		});

		return frame;
	}


	/**
	 * This listener controls the timeline when there are external changes to
	 * the status property. This needs to be done because the
	 * {@link Timeline#statusProperty()} is readonly and can't be bound
	 * bidirectional.
	 */
	private final class StatusChangedListener implements ChangeListener<Status> {
		@Override
		public void changed(ObservableValue<? extends Status> arg0, Status oldStatus, Status newStatus) {

			switch (newStatus) {
			case PAUSED:
				timeline.pause();
				break;
			case RUNNING:
				init();
				timeline.play();
				break;
			case STOPPED:
				timeline.stop();
				break;
			}
		}
	}

	/**
	 * This listener controls the timeline when the desired speed has changed.
	 */
	private final class SpeedChangeListener implements ChangeListener<SpeedLevel> {
		@Override
		public void changed(ObservableValue<? extends SpeedLevel> arg0, SpeedLevel oldSpeed, SpeedLevel newSpeed) {

			Status oldStatus = timeline.getStatus();

			timeline.stop();
			init();

			if (Status.RUNNING.equals(oldStatus)) {
				timeline.play();
			}
		}
	}


	/**
	 * This listener stops the timeline when an collision is detected.
	 */
	private final class CollisionListener implements ChangeListener<Boolean> {
		@Override
		public void changed(final ObservableValue<? extends Boolean> arg0, final Boolean oldValue,
				final Boolean newCollision) {
			if (newCollision) {
				timeline.stop();
			}
		}
	}

}
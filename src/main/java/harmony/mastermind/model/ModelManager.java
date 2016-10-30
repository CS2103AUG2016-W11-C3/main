package harmony.mastermind.model;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import harmony.mastermind.commons.core.ComponentManager;
import harmony.mastermind.commons.core.LogsCenter;
import harmony.mastermind.commons.core.UnmodifiableObservableList;
import harmony.mastermind.commons.events.model.ExpectingConfirmationEvent;
import harmony.mastermind.commons.events.model.TaskManagerChangedEvent;
import harmony.mastermind.commons.events.storage.RelocateFilePathEvent;
import harmony.mastermind.commons.exceptions.FolderDoesNotExistException;
import harmony.mastermind.commons.exceptions.NotRecurringTaskException;
import harmony.mastermind.commons.exceptions.CommandCancelledException;
import harmony.mastermind.commons.util.StringUtil;
import harmony.mastermind.logic.commands.CommandResult;
import harmony.mastermind.logic.commands.Redoable;
import harmony.mastermind.logic.commands.Undoable;
import harmony.mastermind.memory.Memory;
import harmony.mastermind.model.tag.Tag;
import harmony.mastermind.model.task.ArchiveTaskList;
import harmony.mastermind.model.task.ReadOnlyTask;
import harmony.mastermind.model.task.Task;
import harmony.mastermind.model.task.UniqueTaskList;
import harmony.mastermind.model.task.UniqueTaskList.TaskNotFoundException;

/**
 * Represents the in-memory model of the address book data. All changes to any
 * model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final TaskManager taskManager;
    private final FilteredList<Task> filteredTasks;
    private final FilteredList<Task> filteredFloatingTasks;
    private final FilteredList<Task> filteredEvents;
    private final FilteredList<Task> filteredDeadlines;
    private final FilteredList<Task> filteredArchives;
    private final Stack<Undoable> undoHistory;
    private final Stack<Redoable> redoHistory;
    private final Stack<String> commandHistory;

    public static final String TAB_HOME = "Home";
    public static final String TAB_TASKS = "Tasks";
    public static final String TAB_EVENTS = "Events";
    public static final String TAB_DEADLINES = "Deadlines";
    public static final String TAB_ARCHIVES = "Archives";

    private String currentTab;

    /**
     * Initializes a ModelManager with the given TaskManager TaskManager and its
     * variables should not be null
     */
    public ModelManager(TaskManager src, UserPrefs userPrefs) {
        super();
        assert src != null;
        assert userPrefs != null;

        logger.fine("Initializing with task manager: "
                    + src
                    + " and user prefs "
                    + userPrefs);

        taskManager = new TaskManager(src);
        filteredTasks = new FilteredList<>(taskManager.getTasks());
        filteredFloatingTasks = new FilteredList<>(taskManager.getFloatingTasks());
        filteredEvents = new FilteredList<>(taskManager.getEvents());
        filteredDeadlines = new FilteredList<>(taskManager.getDeadlines());
        filteredArchives = new FilteredList<>(taskManager.getArchives());
        undoHistory = new Stack<>();
        redoHistory = new Stack<>();
        commandHistory = new Stack<String>();
        currentTab = TAB_HOME;
    }

    public ModelManager() {
        this(new TaskManager(), new UserPrefs());
    }

    // @@author A0124797R
    public ModelManager(ReadOnlyTaskManager initialData, UserPrefs userPrefs) {
        taskManager = new TaskManager(initialData);
        filteredTasks = new FilteredList<>(taskManager.getTasks());
        filteredFloatingTasks = new FilteredList<>(taskManager.getFloatingTasks());
        filteredEvents = new FilteredList<>(taskManager.getEvents());
        filteredDeadlines = new FilteredList<>(taskManager.getDeadlines());
        filteredArchives = new FilteredList<>(taskManager.getArchives());
        undoHistory = new Stack<>();
        redoHistory = new Stack<>();
        commandHistory = new Stack<String>();
        indicateTaskManagerChanged();
    }

    @Override
    public void resetData(ReadOnlyTaskManager newData) {
        taskManager.resetData(newData);
        clearUndoHistory();
        clearRedoHistory();
        indicateTaskManagerChanged();
    }

    @Override
    public ReadOnlyTaskManager getTaskManager() {
        return taskManager;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTaskManagerChanged() {
        raise(new TaskManagerChangedEvent(taskManager));
    }

    @Override
    // @@author A0124797R
    public void updateCurrentTab(String tab) {
        this.currentTab = tab;
    }

    @Override
    // @@author A0138862W
    /*
     * push the command to the undo history.
     * @see harmony.mastermind.model.Model#pushToUndoHistory(harmony.mastermind.logic.commands.Undoable)
     */
    public void pushToUndoHistory(Undoable command) {
        undoHistory.push(command);
    }

    @Override

    // @@author A0138862W
    /** undo last action performed **/
    public CommandResult undo() throws EmptyStackException {
        CommandResult commandResult = undoHistory.pop().undo();
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
        return commandResult;
    }

    @Override
    // @@author A0138862W
    /*
     * push the command to the redo history. Should only be called after an undo operation
     * @see harmony.mastermind.model.Model#pushToRedoHistory(harmony.mastermind.logic.commands.Redoable)
     */
    public void pushToRedoHistory(Redoable command) {
        redoHistory.push(command);
    }

    @Override
    // @@author A0138862W
    /** redo the action that being undone function **/
    public CommandResult redo() throws EmptyStackException {
        CommandResult commandResult = redoHistory.pop().redo();
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
        return commandResult;
    }

    @Override
    /**
     * This method should only be called when the user entered a new command
     * other than redo/undo
     **/
    // @@author A0138862W
    public void clearRedoHistory() {
        redoHistory.clear();
    }

    // @@author A0139194X
    /**
     * This method should only be called when the user entered a new command
     * other than redo/undo
     **/
    public void clearUndoHistory() {
        undoHistory.clear();
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask target) throws TaskNotFoundException {
        taskManager.removeTask(target);
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void deleteArchive(ReadOnlyTask target) throws TaskNotFoundException, ArchiveTaskList.TaskNotFoundException {
        taskManager.removeArchive(target);
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        System.out.println("inside adding");
        taskManager.addTask(task);
        System.out.println("after adding");
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    // @@author A0124797R
    public synchronized void markTask(Task target) throws TaskNotFoundException {
        taskManager.markTask(target);
        indicateTaskManagerChanged();
    }

    @Override
    // @@author A0124797R
    public synchronized void unmarkTask(Task target) throws ArchiveTaskList.TaskNotFoundException, UniqueTaskList.DuplicateTaskException {
        taskManager.unmarkTask(target);
        indicateTaskManagerChanged();
    }
    

    // =========== Filtered List Accessors
    // ===============================================================
    @Override
    // @@author A0124797R
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredFloatingTaskList() {
        return new UnmodifiableObservableList<>(taskManager.getFloatingTasks());
    }

    // @@author A0124797R
    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredEventList() {
        return new UnmodifiableObservableList<>(taskManager.getEvents());
    }

    // @@author A0124797R
    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredDeadlineList() {
        return new UnmodifiableObservableList<>(taskManager.getDeadlines());
    }

    // @@author A0124797R
    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredArchiveList() {
        return new UnmodifiableObservableList<>(taskManager.getArchives());
    }
    
    // =========== Methods for file access ================================

    @Override
    // @@author A0124797R
    public synchronized BufferedReader importFile(String fileToImport) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(fileToImport));
        
        return br;
    }

    // @@author: A0139194X
    @Override
    public synchronized void relocateSaveLocation(String newFilePath) throws FolderDoesNotExistException {
        raise(new RelocateFilePathEvent(newFilePath));
        indicateTaskManagerChanged();
    }

    public synchronized void indicateConfirmationToUser() throws CommandCancelledException {
        raise(new ExpectingConfirmationEvent());
    }
    
    // =========== Methods for Recurring Tasks=============================

    @Override
    // @@author A0124797R
    public synchronized void addNextTask(Task task) throws UniqueTaskList.DuplicateTaskException, NotRecurringTaskException {
        taskManager.addNextTask(task);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    // =========== Filtered Task List Accessors
    // ===============================================================

    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTaskList() {
        return new UnmodifiableObservableList<>(filteredTasks);
    }

    @Override
    // @@author A0124797R
    public ObservableList<Task> getListToMark() {
        return getCurrentObservableList();
    }

    @Override
    // @@author A0124797R
    public void updateFilteredListToShowAll() {
        switch (currentTab) {
            case TAB_HOME:
                filteredTasks.setPredicate(null);
                break;
            case TAB_TASKS:
                filteredFloatingTasks.setPredicate(null);
                break;
            case TAB_EVENTS:
                filteredEvents.setPredicate(null);
                break;
            case TAB_DEADLINES:
                filteredDeadlines.setPredicate(null);
                break;
            case TAB_ARCHIVES:
                filteredArchives.setPredicate(null);
                break;
        }
    }
    
    @Override
    // @@author A0124797R
    public void updateFilteredListToShow(String tab) {
        switch (tab) {
            case TAB_HOME:
                currentTab = TAB_HOME;
                break;
            case TAB_TASKS:
                currentTab = TAB_TASKS;
                break;
            case TAB_EVENTS:
                currentTab = TAB_EVENTS;
                break;
            case TAB_DEADLINES:
                currentTab = TAB_DEADLINES;
                break;
            case TAB_ARCHIVES:
                currentTab = TAB_ARCHIVES;
                break;
        }
        updateFilteredListToShowAll();
    }

    @Override
    //@@author A0124797R
    public void updateFilteredListToShowUpcoming(long time, String taskType) {
        updateFilteredTaskList(new PredicateExpression(new DateQualifier(time, taskType)));
    }

    @Override
    public void updateFilteredTaskList(Set<String> keywords) {
        updateFilteredTaskList(new PredicateExpression(new NameQualifier(keywords)));
    }

    @Override
    // @@author A0124797R
    public void updateFilteredTagTaskList(Set<Tag> keywords) {
        updateFilteredTaskList(new PredicateExpression(new TagQualifier(keywords)));
    }

    private void updateFilteredTaskList(Expression expression) {
        filteredTasks.setPredicate(expression::satisfies);
    }

    // @@author A0124797R
    private ObservableList<Task> getCurrentObservableList() {
        ObservableList<Task> list = filteredTasks;

        switch (currentTab) {
            case "Home":
                list = filteredTasks;
                break;
            case "Tasks":
                list = filteredFloatingTasks;
                break;
            case "Events":
                list = filteredEvents;
                break;
            case "Deadlines":
                list = filteredDeadlines;
                break;
            case "Archives":
                list = filteredArchives;
                break;
        }

        return list;
    }

    @Override
    // @@author A0124797R
    public UnmodifiableObservableList<ReadOnlyTask> getCurrentList() {
        return new UnmodifiableObservableList<ReadOnlyTask>(getCurrentObservableList());
    }

    private void searchTask(String keyword, Memory memory) {
        taskManager.searchTask(keyword, memory);
    }

    // ========== Inner classes/interfaces used for filtering
    // ==================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);

        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);

        String toString();
    }

    private class NameQualifier implements Qualifier {
        private Set<String> nameKeyWords;

        NameQualifier(Set<String> nameKeyWords) {
            this.nameKeyWords = nameKeyWords;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            return nameKeyWords.stream().filter(keyword -> StringUtil.containsIgnoreCase(task.getName(), keyword)).findAny().isPresent();
        }

        @Override
        public String toString() {
            return "name="
                   + String.join(", ", nameKeyWords);
        }
    }

    // @@author A0124797R
    private class TagQualifier implements Qualifier {
        private Set<Tag> tagKeyWords;

        TagQualifier(Set<Tag> tagKeyWords) {
            this.tagKeyWords = tagKeyWords;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            final Set<Tag> tagList = task.getTags().toSet();

            return !Collections.disjoint(tagList, tagKeyWords);
        }

        @Override
        public String toString() {
            return "tags="
                   + String.join(", ", tagKeyWords.toString());
        }
    }

    private class DateQualifier implements Qualifier {
        private final String NAME_DEADLINE = "deadlines";
        private final String NAME_EVENT = "events";
        
        private long timeNow;
        private long oneWeekFromNow;
        private final long oneWeek = 604800000;
        private String taskType;

        DateQualifier(long time, String taskType) {
            this.timeNow = new Date().getTime();
            this.oneWeekFromNow = time + oneWeek;
            this.taskType = taskType;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            switch (taskType) {
                case NAME_DEADLINE: return isUpcomingDeadline(task);
                case NAME_EVENT:    return isUpcomingEvent(task);
                default:            return isUpcomingAll(task);
            }
        }

        @Override
        public String toString() {
            return "Date before:"
                   + oneWeekFromNow;
        }
        
        private boolean isUpcomingAll(ReadOnlyTask task) {
            if (task.isFloating()) {
                return true;
            } else {
                return isUpcoming(task);
            }
        }
        
        private boolean isUpcomingEvent(ReadOnlyTask task) {
            if (task.isFloating() || task.isDeadline()) {
                return false;
            } else {
                return isUpcoming(task);
            }
        }
        
        
        private boolean isUpcomingDeadline(ReadOnlyTask task) {
            if (task.isFloating() || task.isEvent()) {
                return false;
            } else {
                return isUpcoming(task);
            }
        }
        
        
        /**
         * Checks if end date of task is within one week from now.
         */
        private boolean isUpcoming(ReadOnlyTask task) {
            long taskTime = task.getEndDate().getTime();
            boolean isUpcoming = taskTime < oneWeekFromNow && taskTime > timeNow;
            return isUpcoming;
        }
    }

    @Override
    public void searchTask(String input) {
        // implementing next milestone
    }

}

package harmony.mastermind.logic;

import com.google.common.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import harmony.mastermind.commons.core.EventsCenter;
import harmony.mastermind.logic.commands.*;
import harmony.mastermind.model.Model;
import harmony.mastermind.model.ModelManager;
import harmony.mastermind.model.ReadOnlyTaskManager;
import harmony.mastermind.model.TaskManager;
import harmony.mastermind.model.tag.Tag;
import harmony.mastermind.model.tag.UniqueTagList;
import harmony.mastermind.model.task.ReadOnlyTask;
import harmony.mastermind.model.task.Task;
import harmony.mastermind.storage.StorageManager;
import harmony.mastermind.commons.events.model.TaskManagerChangedEvent;
import harmony.mastermind.commons.events.ui.JumpToListRequestEvent;
import harmony.mastermind.commons.events.ui.ShowHelpRequestEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static harmony.mastermind.commons.core.Messages.*;

public class LogicManagerTest {

    /**
     * See https://github.com/junit-team/junit4/wiki/rules#temporaryfolder-rule
     */
    @Rule
    public TemporaryFolder saveFolder = new TemporaryFolder();

    private Model model;
    private Logic logic;

    //These are for checking the correctness of the events raised
    private ReadOnlyTaskManager latestSavedAddressBook;
    private boolean helpShown;
    private int targetedJumpIndex;

    @Subscribe
    private void handleLocalModelChangedEvent(TaskManagerChangedEvent abce) {
        latestSavedAddressBook = new TaskManager(abce.data);
    }

    @Subscribe
    private void handleShowHelpRequestEvent(ShowHelpRequestEvent she) {
        helpShown = true;
    }

    @Subscribe
    private void handleJumpToListRequestEvent(JumpToListRequestEvent je) {
        targetedJumpIndex = je.targetIndex;
    }

    @Before
    public void setup() {
        model = new ModelManager();
        String tempAddressBookFile = saveFolder.getRoot().getPath() + "TempAddressBook.xml";
        String tempPreferencesFile = saveFolder.getRoot().getPath() + "TempPreferences.json";
        logic = new LogicManager(model, new StorageManager(tempAddressBookFile, tempPreferencesFile));
        EventsCenter.getInstance().registerHandler(this);

        latestSavedAddressBook = new TaskManager(model.getTaskManager()); // last saved assumed to be up to date
        helpShown = false;
        targetedJumpIndex = -1; // non yet
    }

    @After
    public void teardown() {
        EventsCenter.clearSubscribers();
    }

    @Test
    public void execute_invalid() throws Exception {
        String invalidCommand = "       ";
        assertCommandBehavior(invalidCommand,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
    }

    /**
     * Executes the command and confirms that the result message is correct.
     * Both the 'address book' and the 'last shown list' are expected to be empty.
     * @see #assertCommandBehavior(String, String, ReadOnlyAddressBook, List)
     */
    private void assertCommandBehavior(String inputCommand, String expectedMessage) throws Exception {
        assertCommandBehavior(inputCommand, expectedMessage, new TaskManager(), Collections.emptyList());
    }

    /**
     * Executes the command and confirms that the result message is correct and
     * also confirms that the following three parts of the LogicManager object's state are as expected:<br>
     *      - the internal address book data are same as those in the {@code expectedAddressBook} <br>
     *      - the backing list shown by UI matches the {@code shownList} <br>
     *      - {@code expectedAddressBook} was saved to the storage file. <br>
     */
    private void assertCommandBehavior(String inputCommand, String expectedMessage,
                                       ReadOnlyTaskManager expectedAddressBook,
                                       List<? extends ReadOnlyTask> expectedShownList) throws Exception {

        //Execute the command
        CommandResult result = logic.execute(inputCommand);

        //Confirm the ui display elements should contain the right data
        assertEquals(expectedMessage, result.feedbackToUser);
        assertEquals(expectedShownList, model.getFilteredTaskList());

        //Confirm the state of data (saved and in-memory) is as expected
        assertEquals(expectedAddressBook, model.getTaskManager());
        assertEquals(expectedAddressBook, latestSavedAddressBook);
    }


    @Test
    public void execute_unknownCommandWord() throws Exception {
        String unknownCommand = "uicfhmowqewca";
        assertCommandBehavior(unknownCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_help() throws Exception {
        assertCommandBehavior("help", HelpCommand.SHOWING_HELP_MESSAGE);
        assertTrue(helpShown);
    }

    @Test
    public void execute_exit() throws Exception {
        assertCommandBehavior("exit", ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT);
    }

    @Test
    public void execute_clear() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        model.addTask(helper.generateTask(1));
        model.addTask(helper.generateTask(2));
        model.addTask(helper.generateTask(3));

        assertCommandBehavior("clear", ClearCommand.MESSAGE_SUCCESS, new TaskManager(), Collections.emptyList());
    }


    @Test
    public void execute_add_invalidArgsFormat() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_EXAMPLES);
        assertCommandBehavior("add wrong args wrong args", expectedMessage);
        assertCommandBehavior("add Valid Name 12345 e/valid@email.butNoPhonePrefix a/valid, address", expectedMessage);
        assertCommandBehavior("add Valid Name p/12345 valid@email.butNoPrefix a/valid, address", expectedMessage);
        assertCommandBehavior("add Valid Name p/12345 e/valid@email.butNoAddressPrefix valid, address",
                              expectedMessage);
    }

    @Test
    public void execute_add_successful() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.task();
        TaskManager expectedAB = new TaskManager();
        expectedAB.addTask(toBeAdded);

        // execute command and verify result
        assertCommandBehavior(helper.generateAddCommand(toBeAdded),
                String.format(AddCommand.MESSAGE_SUCCESS, toBeAdded),
                expectedAB,
                expectedAB.getTaskList());

    }
    
    @Test
    //@@author A0138862W
    public void execute_undoAndRedo_add() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.task();

        logic.execute(helper.generateAddCommand(toBeAdded));
        
        assertCommandBehavior("undo", "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "[Undo Add Command] Task deleted: task Tags: [tag1],[tag2]\n"
                + "==================",
                model.getTaskManager(),
                model.getTaskManager().getTaskList());
        
        assertCommandBehavior("redo", "Redo successfully.\n"
                + "=====Redo Details=====\n"
                + "[Redo Add Command] Task added: task Tags: [tag1],[tag2]\n"
                + "==================",
                model.getTaskManager(),
                model.getTaskManager().getTaskList());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undoAndRedo_edit() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeEdited = helper.task();
        List<Task> onePerson = helper.generateTaskList(toBeEdited);
        TaskManager expectedTM = helper.generateTaskManager(onePerson);
        List<Task>expectedList = onePerson;
        
        helper.addToModel(model, onePerson);

        logic.execute(helper.generateEditCommand());
        
        assertCommandBehavior("undo",
                "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "[Undo Edit Command] Task reverted: task Tags: [tag1],[tag2]\n"
                + "==================",       
                expectedTM,
                expectedList);
        
        assertCommandBehavior("redo",
                "Redo successfully.\n"
                + "=====Redo Details=====\n"
                + "[Redo Edit Command] Edit the following task: task Tags: [tag1],[tag2]\n"
                + "==================",       
                expectedTM,
                expectedList);
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_delete() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeEdited = helper.task();
        List<Task> onePerson = helper.generateTaskList(toBeEdited);
        TaskManager expectedTM = helper.generateTaskManager(onePerson);
        List<Task>expectedList = onePerson;
        
        helper.addToModel(model, onePerson);

        logic.execute("delete 1");
        
        assertCommandBehavior("undo",
                "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "[Undo Delete Command] Task added: task Tags: [tag1],[tag2]\n"
                + "==================",       
                expectedTM,
                expectedList);
        
        assertCommandBehavior("redo",
                "Redo successfully.\n"
                + "=====Redo Details=====\n"
                + "[Redo Delete Command] Deleted Task: task Tags: [tag1],[tag2]\n"
                + "==================",
                model.getTaskManager(),
                model.getListToMark());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_mark() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeEdited = helper.task();
        List<Task> onePerson = helper.generateTaskList(toBeEdited);
        TaskManager expectedTM = helper.generateTaskManager(onePerson);
        List<Task>expectedList = onePerson;
        
        helper.addToModel(model, onePerson);

        logic.execute("mark 1");
        
        assertCommandBehavior("undo",
                "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "[Undo Mark Command] task Tags: [tag1],[tag2] has been unmarked\n"
                + "==================",       
                expectedTM,
                expectedList);
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_unmark() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeEdited = helper.task();
        List<Task> onePerson = helper.generateTaskList(toBeEdited);
        TaskManager expectedTM = helper.generateTaskManager(onePerson);
        List<Task>expectedList;
        
        helper.addToModel(model, onePerson);
        
        logic.execute("mark 1");
        
        logic.execute("unmark 1");
        
        assertCommandBehavior("undo",
                "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "[Undo Unmark Command] task Tags: [tag1],[tag2] has been archived\n"
                + "==================",       
                model.getTaskManager(),
                model.getListToMark());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_invalidAddTaskNotFound() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.task();

        logic.execute(helper.generateAddCommand(toBeAdded));
        
        model.deleteTask(toBeAdded);
        
        assertCommandBehavior("undo", "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "Task could not be found in Mastermind\n"
                + "==================",
                model.getTaskManager(),
                model.getTaskManager().getTaskList());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_invalidEditTaskNotFound() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task toBeEdited = helper.task();
        List<Task> onePerson = helper.generateTaskList(toBeEdited);
        helper.addToModel(model, onePerson);

        logic.execute(helper.generateEditCommand());
        
        model.deleteTask(toBeEdited);
        
        assertCommandBehavior("undo", "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "Task could not be found in Mastermind\n"
                + "==================",
                model.getTaskManager(),
                model.getFilteredTaskList());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_invalidEditDuplicate() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task task1 = helper.generateTaskWithName("edited2 task name");
        Task task2 = helper.generateTaskWithName("edited2 task name");
        
        List<Task> twoTasks = helper.generateTaskList(task1);
        TaskManager expectedTM = helper.generateTaskManager(twoTasks);
        List<Task>expectedList = twoTasks;
        
        model.addTask(task1);

        logic.execute(helper.generateEditCommand());

        model.getTaskManager().getUniqueTaskList().getInternalList().add(task1);
        model.getTaskManager().getUniqueTaskList().getInternalList().add(task2);
        
        assertCommandBehavior("undo", "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "This task already exists in Mastermind\n"
                + "==================",
                model.getTaskManager(),
                model.getFilteredTaskList());
    }
    
    @Test
    //@@author A0138862W
    public void execute_undo_invalidDeleteDuplicate() throws Exception{
        TestDataHelper helper = new TestDataHelper();
        Task task1 = helper.generateTaskWithName("edited2 task name");
        Task task2 = helper.generateTaskWithName("edited2 task name");
        
        List<Task> twoTasks = helper.generateTaskList(task1);
        
        model.addTask(task1);

        logic.execute("delete 1");

        model.getTaskManager().getUniqueTaskList().getInternalList().add(task1);
        model.getTaskManager().getUniqueTaskList().getInternalList().add(task2);
        
        assertCommandBehavior("undo", "Undo successfully.\n"
                + "=====Undo Details=====\n"
                + "This task already exists in Mastermind\n"
                + "==================",
                model.getTaskManager(),
                model.getFilteredTaskList());
    }

    /*
    @Test
    public void execute_addDuplicate_notAllowed() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.task();
        TaskManager expectedAB =        new TaskManager();
        expectedAB.addTask(toBeAdded);

        // setup starting state
        model.addTask(toBeAdded); // person already in internal address book

        // execute command and verify result
        assertCommandBehavior(
                helper.generateAddCommand(toBeAdded),
                AddCommand.MESSAGE_DUPLICATE_TASK,
                expectedAB,
                expectedAB.getTaskList());

    }
*/

    @Test
    public void execute_list_showsAllTasks() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        TaskManager expectedAB = helper.generateTaskManager(2);
        List<? extends ReadOnlyTask> expectedList = expectedAB.getTaskList();

        // prepare address book state
        helper.addToModel(model, 2);

        assertCommandBehavior("list",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                expectedList);
    }


    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single person in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single person in the last shown list
     *                    based on visible index.
     */
    private void assertIncorrectIndexFormatBehaviorForCommand(String commandWord, String expectedMessage)
            throws Exception {
        assertCommandBehavior(commandWord , expectedMessage); //index missing
        assertCommandBehavior(commandWord + " +1", expectedMessage); //index should be unsigned
        assertCommandBehavior(commandWord + " -1", expectedMessage); //index should be unsigned
        assertCommandBehavior(commandWord + " 0", expectedMessage); //index cannot be 0
        assertCommandBehavior(commandWord + " not_a_number", expectedMessage);
    }

    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single person in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single person in the last shown list
     *                    based on visible index.
     */
    private void assertIndexNotFoundBehaviorForCommand(String commandWord) throws Exception {
        String expectedMessage = MESSAGE_INVALID_TASK_DISPLAYED_INDEX;
        TestDataHelper helper = new TestDataHelper();
        List<Task> personList = helper.generateTaskList(2);

        // set AB state to 2 persons
        model.resetData(new TaskManager());
        for (Task p : personList) {
            model.addTask(p);
        }

        assertCommandBehavior(commandWord + " 3", expectedMessage, model.getTaskManager(), personList);
    }
    /*
    @Test
    public void execute_selectInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SelectCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("select", expectedMessage);
    }*/
/*
    @Test
    public void execute_selectIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("select");
    }
    */
    /*
    @Test
    public void execute_select_jumpsToCorrectPerson() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Person> threePersons = helper.generateTaskList(3);

        AddressBook expectedAB = helper.generateTaskManager(threePersons);
        helper.addToModel(model, threePersons);

        assertCommandBehavior("select 2",
                String.format(SelectCommand.MESSAGE_SELECT_TASK_SUCCESS, 2),
                expectedAB,
                expectedAB.getPersonList());
        assertEquals(1, targetedJumpIndex);
        assertEquals(model.getFilteredTaskList().get(1), threePersons.get(1));
    }
     */

    @Test
    public void execute_deleteInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("delete", expectedMessage);
    }

    @Test
    public void execute_deleteIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("delete");
    }

    @Test
    public void execute_delete_removesCorrectPerson() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateTaskList(3);

        TaskManager expectedAB = helper.generateTaskManager(threePersons);
        expectedAB.removeTask(threePersons.get(1));
        helper.addToModel(model, threePersons);

        assertCommandBehavior("delete 2",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, threePersons.get(1)),
                expectedAB,
                expectedAB.getTaskList());
    }


    @Test
    public void execute_find_invalidArgsFormat() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertCommandBehavior("find ", expectedMessage);
    }

    @Test
    public void execute_find_onlyMatchesFullWordsInNames() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task pTarget1 = helper.generateTaskWithName("bla bla KEY bla");
        Task pTarget2 = helper.generateTaskWithName("bla KEY bla bceofeia");
        Task p1 = helper.generateTaskWithName("KE Y");
        Task p2 = helper.generateTaskWithName("KEYKEYKEY sduauo");

        List<Task> fourPersons = helper.generateTaskList(p1, pTarget1, p2, pTarget2);
        TaskManager expectedAB = helper.generateTaskManager(fourPersons);
        List<Task> expectedList = helper.generateTaskList(pTarget1, pTarget2);
        helper.addToModel(model, fourPersons);

        assertCommandBehavior("find KEY",
                Command.getMessageForTaskListShownSummary(expectedList.size()),
                expectedAB,
                expectedList);
    }

    @Test
    public void execute_find_isNotCaseSensitive() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task p1 = helper.generateTaskWithName("bla bla KEY bla");
        Task p2 = helper.generateTaskWithName("bla KEY bla bceofeia");
        Task p3 = helper.generateTaskWithName("key key");
        Task p4 = helper.generateTaskWithName("KEy sduauo");

        List<Task> fourPersons = helper.generateTaskList(p3, p1, p4, p2);
        TaskManager expectedAB = helper.generateTaskManager(fourPersons);
        List<Task> expectedList = fourPersons;
        helper.addToModel(model, fourPersons);

        assertCommandBehavior("find KEY",
                Command.getMessageForTaskListShownSummary(expectedList.size()),
                expectedAB,
                expectedList);
    }

    @Test
    public void execute_find_matchesIfAnyKeywordPresent() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task pTarget1 = helper.generateTaskWithName("bla bla KEY bla");
        Task pTarget2 = helper.generateTaskWithName("bla rAnDoM bla bceofeia");
        Task pTarget3 = helper.generateTaskWithName("key key");
        Task p1 = helper.generateTaskWithName("sduauo");

        List<Task> fourPersons = helper.generateTaskList(pTarget1, p1, pTarget2, pTarget3);
        TaskManager expectedAB = helper.generateTaskManager(fourPersons);
        List<Task> expectedList = helper.generateTaskList(pTarget1, pTarget2, pTarget3);
        helper.addToModel(model, fourPersons);

        assertCommandBehavior("find key rAnDoM",
                Command.getMessageForTaskListShownSummary(expectedList.size()),
                expectedAB,
                expectedList);
    }


    /**
     * A utility class to generate test data.
     */
    class TestDataHelper{

        Task task() throws Exception {
            String name = "task";
            Date startDate = new Date();
            Date endDate = new Date();
            Tag tag1 = new Tag("tag1");
            Tag tag2 = new Tag("tag2");
            UniqueTagList tags = new UniqueTagList(tag1, tag2);
            
            return new Task(name, startDate, endDate, tags);
        }

        /**
         * Generates a valid person using the given seed.
         * Running this function with the same parameter values guarantees the returned person will have the same state.
         * Each unique seed will generate a unique Person object.
         *
         * @param seed used to generate the person data field values
         */
        Task generateTask(int seed) throws Exception {
            
            return new Task(
                    "task"+seed,
                    new Date(),
                    new Date(),
                    new UniqueTagList(new Tag("tag" + Math.abs(seed)), new Tag("tag" + Math.abs(seed + 1)))
                    );
        }

        /** Generates the correct add command based on the person given */
        String generateAddCommand(Task p) {
            StringBuffer cmd = new StringBuffer();

            cmd.append("add");

            cmd.append(" '").append(p.getName().toString()+"'");
            cmd.append(" sd/'").append(p.getStartDate()+"'");
            cmd.append(" ed/'").append(p.getEndDate()+"'");
            cmd.append(" t/'");

            UniqueTagList tags = p.getTags();
            for (Tag t: tags) {
                cmd.append(t.tagName);
                cmd.append(',');
            }
            
            cmd.deleteCharAt(cmd.length()-1);
            cmd.append("'");

            return cmd.toString();
        }
        
        String generateEditCommand() {
            StringBuffer cmd = new StringBuffer();

            cmd.append("edit 1");

            cmd.append(" name/\"edited task name\"");
            
            cmd.deleteCharAt(cmd.length()-1);

            return cmd.toString();
        }

        /**
         * Generates an AddressBook with auto-generated persons.
         */
        TaskManager generateTaskManager(int numGenerated) throws Exception{
            TaskManager addressBook = new TaskManager();
            addToTaskManager(addressBook, numGenerated);
            return addressBook;
        }

        /**
         * Generates an AddressBook based on the list of Persons given.
         */
        TaskManager generateTaskManager(List<Task> persons) throws Exception{
            TaskManager addressBook = new TaskManager();
            addToTaskManager(addressBook, persons);
            return addressBook;
        }

        /**
         * Adds auto-generated Person objects to the given AddressBook
         * @param addressBook The AddressBook to which the Persons will be added
         */
        void addToTaskManager(TaskManager addressBook, int numGenerated) throws Exception {
            addToTaskManager(addressBook, generateTaskList(numGenerated));
        }

        /**
         * Adds the given list of Persons to the given AddressBook
         */
        void addToTaskManager(TaskManager addressBook, List<Task> personsToAdd) throws Exception {
            for (Task p: personsToAdd) {
                addressBook.addTask(p);
            }
        }

        /**
         * Adds auto-generated Person objects to the given model
         * @param model The model to which the Persons will be added
         */
        void addToModel(Model model, int numGenerated) throws Exception{
            addToModel(model, generateTaskList(numGenerated));
        }

        /**
         * Adds the given list of Persons to the given model
         */
        void addToModel(Model model, List<Task> personsToAdd) throws Exception{
            for (Task p: personsToAdd) {
                model.addTask(p);
            }
        }

        /**
         * Generates a list of Persons based on the flags.
         */
        List<Task> generateTaskList(int numGenerated) throws Exception{
            List<Task> persons = new ArrayList<>();
            for (int i = 1; i <= numGenerated; i++) {
                persons.add(generateTask(i));
            }
            return persons;
        }

        List<Task> generateTaskList(Task... persons) {
            return Arrays.asList(persons);
        }

        /**
         * Generates a Person object with given name. Other fields will have some dummy values.
         */
        Task generateTaskWithName(String name) throws Exception {
            return new Task(
                    name,
                    new Date(),
                    new Date(),
                    new UniqueTagList(new Tag("tag1"), new Tag("tag2"))
            );
        }
    }
}

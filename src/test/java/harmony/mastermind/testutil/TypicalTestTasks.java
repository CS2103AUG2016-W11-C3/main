package harmony.mastermind.testutil;

import java.util.List;

import harmony.mastermind.commons.exceptions.IllegalValueException;
import harmony.mastermind.model.TaskManager;
import harmony.mastermind.model.task.*;
import harmony.mastermind.model.task.UniqueTaskList.TaskNotFoundException;

/**
 *
 */
public class TypicalTestTasks {

    public static TestTask task1, task2, task3, task4, task5, task6, task7, task8;

    //@@author A0124797R
    public TypicalTestTasks() {
        
        try {
            task1 =  new TaskBuilder().withName("do laundry")
                    .withTags("chores").build();
            task2 = new TaskBuilder().withName("finish assignment").build();
            task3 = new TaskBuilder().withName("cs2105 assignment")
                    .withStartDate("today").withEndDate("tomorrow")
                    .withTags("examPrep").build();
            task4 = new TaskBuilder().withName("complete cs2103 lecture quiz")
                    .withEndDate("tomorrow")
                    .withTags("homework").build();
            
            
            //manual inputs
            task5 = new TaskBuilder().withName("do past year papers")
                    .withEndDate("tomorrow")
                    .withTags("homework").build();
            task6 = new TaskBuilder().withName("sweep floor").withEndDate("friday").build();
            
            //completed tasks
            task7 = new TaskBuilder().withName("lecture").build();
            task8 = new TaskBuilder().withName("submit PR").withEndDate("today").build();
            
        } catch (IllegalValueException e) {
            e.printStackTrace();
            assert false : "not possible";
        }
        
    }

    //@@author A0124797R
    public static void loadTaskManagerWithSampleData(TaskManager tm) {

        try {
            tm.addTask(new Task(task1));
            tm.addTask(new Task(task2));
            tm.addTask(new Task(task3));
            tm.addTask(new Task(task4));
            tm.addTask(new Task(task7));
            tm.markTask(new Task(task7));
            tm.addTask(new Task(task8));
            tm.markTask(new Task(task8));
            
        } catch (UniqueTaskList.DuplicateTaskException e) {
            assert false : "not possible";
        } catch (TaskNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //@@author A0124797R
    public TestTask[] getTypicalTasks() {
        return new TestTask[]{task1, task2, task3, task4};
    }
    
    //@@author A0124797R
    public TestTask[] getTypicalArchivedTasks() {
        return new TestTask[]{task7.mark(),task8.mark()};
    }

    public TaskManager getTypicalTaskManager(){
        TaskManager tm = new TaskManager();
        loadTaskManagerWithSampleData(tm);
        return tm;
    }
}
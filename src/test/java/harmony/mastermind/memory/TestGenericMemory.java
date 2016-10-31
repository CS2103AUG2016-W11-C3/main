package harmony.mastermind.memory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import harmony.mastermind.memory.GenericMemory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestGenericMemory {
    
    private Calendar start = new GregorianCalendar();
    private Calendar end = new GregorianCalendar();
    
    @Test
    //@@author A0143378Y
    public void test() { 
        start.set(2014, 03, 27, 03, 14, 20);
        end.set(2015, 02, 05, 07, 11, 13);
        
        addEvent();
        addDeadline();
        addTask();
        
        testOtherMethods();
    }
    
    //@@author A0143378Y
    private void addEvent() { 
        GenericMemory testEvent = new GenericMemory("event", "name", "description", start, end, 0);
        assertEquals("Type", testEvent.getType(), "event");
        assertEquals("Name", testEvent.getName(), "name");
        assertEquals("Description", testEvent.getDescription(), "description");
        assertEquals("start time and date", testEvent.getStart(), start);
        assertEquals("end time and date", testEvent.getEnd(), end);
    }
    
    //@@author A0143378Y
    private void addDeadline() { 
        GenericMemory testDeadline = new GenericMemory("deadline", "name1", "description1", end);
        assertEquals("Type", testDeadline.getType(), "deadline");
        assertEquals("Name", testDeadline.getName(), "name1");
        assertEquals("Description", testDeadline.getDescription(), "description1");
        assertEquals("start time and date", testDeadline.getStart(), null);
        assertEquals("end time and date", testDeadline.getEnd(), end);
    }
    
    //@@author A0143378Y
    private void addTask() { 
        GenericMemory testTask = new GenericMemory("task", "name2", "description2");
        assertEquals("Type", testTask.getType(), "task");
        assertEquals("Name", testTask.getName(), "name2");
        assertEquals("Description", testTask.getDescription(), "description2");
        assertEquals("start time and date", testTask.getStart(), null);
        assertEquals("end time and date", testTask.getEnd(), null);
    }
    
    //@@author A0143378Y
    private void testOtherMethods() { 
        GenericMemory testMethod = new GenericMemory("Method", "name3", "description3");
        assertEquals("start time and date", testMethod.getStart(), null);
        testMethod.initStart();
        assertFalse("start time and date", testMethod.getStart() == null);
        
        assertEquals("end time and date", testMethod.getEnd(), null);
        testMethod.initEnd();
        assertFalse("start time and date", testMethod.getEnd() == null);
        
        testMethod.setType("cat");
        assertEquals("type", testMethod.getType(), "cat");
        
        testMethod.setName("Animals");
        assertEquals("Name", testMethod.getName(),"Animals");
        
        testMethod.setDescription("An organism");
        assertEquals("Description", testMethod.getDescription(), "An organism");
        
        testMethod.setStartTime(03,  14);
        testMethod.setEndTime(07, 11);
        
        testMethod.setStartDate(2014, 03, 27);
        testMethod.setEndDate(2015, 02, 05);
    }
}
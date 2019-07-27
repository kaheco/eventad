import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.atomic.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataAccessJSON implements IDataAccess {

    private final String STUDENTS_JSON = "students.json";
    private final String COURSES_JSON = "courses.json";
    private int lastCourseID;
    private int lastStudentID;
    private static AtomicInteger idCounter;

    private static String generateID(int lastID) {
        idCounter = new AtomicInteger(lastID);
        return String.valueOf(idCounter.incrementAndGet());
    }

    private int readLastID(JSONObject wrapperObj, String type) {
        JSONObject concreteObj = (JSONObject) wrapperObj.get(type);
        String ID = (String) concreteObj.get("ID");
        return Integer.parseInt(ID);
    }

    private JSONArray loadExistingCourses() {
        JSONArray courseObjects = this.readJSON(COURSES_JSON);
        int lastIndex = courseObjects.size() - 1;
        if (lastIndex < 0) {
            lastCourseID = 0;
        } else {
            lastCourseID = readLastID((JSONObject) courseObjects.get(lastIndex), "course");
        }
        return courseObjects;
    }

    private JSONArray loadExistingStudents() {
        JSONArray studentObjects = this.readJSON(STUDENTS_JSON);
        int lastIndex = studentObjects.size() - 1;
        if (lastIndex < 0) {
            lastStudentID = 0;
        } else {
            lastStudentID = readLastID((JSONObject) studentObjects.get(lastIndex), "student");
        }
        return studentObjects;
    }

    private ArrayList<Student> getStudentsByCourse(Course course) {
        JSONArray studentsList = this.loadExistingStudents();
        ArrayList<Student> parsedStudents = this.parseAllStudents(studentsList);
        ArrayList<Student> matchingStudents = new ArrayList<>();
        for (Student studentObj : parsedStudents) {
            if (studentObj.getCourse().equals(course.getName()))
                matchingStudents.add(studentObj);
        }
        return matchingStudents;
    }

    private JSONArray readJSON(String path) {
        JSONArray jsonObjects = new JSONArray();
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(path)) {
            Object parsedObj = jsonParser.parse(reader);
            jsonObjects = (JSONArray) parsedObj;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonObjects;
    }

    private void writeJSON(String path, JSONArray list) {
        try (FileWriter file = new FileWriter(path)) {
            file.write(list.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject prepareJsonObject(Object object, boolean addOperation) {
        JSONObject jsonObj = new JSONObject();
        JSONObject jsonObjWrapper = new JSONObject();
        if (object instanceof Course) {
            Course course = (Course) object;
            jsonObj.put("ID", addOperation ? generateID(lastCourseID) : Integer.toString(course.getId()));
            jsonObj.put("name", course.getName());
            jsonObjWrapper.put("course", jsonObj);
        } else {
            Student student = (Student) object;
            jsonObj.put("ID", addOperation ? generateID(lastStudentID) : Integer.toString(student.getId()));
            jsonObj.put("firstname", student.getFirstname());
            jsonObj.put("lastname", student.getLastname());
            jsonObj.put("course", student.getCourse() != null ? student.getCourse() : "");
            jsonObjWrapper.put("student", jsonObj);
        }
        return jsonObjWrapper;
    }

    @SuppressWarnings("unchecked")
    private void updateRelatedStudents(Course course) {
        boolean match;
        JSONArray updatedStudents = new JSONArray();
        ArrayList<Student> allStudents = this.parseAllStudents(this.loadExistingStudents());
        for (Student student : allStudents) {
            match = false;
            for (Student relatedStudent : course.getStudents()) {
                if (relatedStudent.getId() == student.getId()) {
                    match = true;
                    updatedStudents.add(prepareJsonObject(relatedStudent, false));
                    break;
                }
            }
            if(!match)
                updatedStudents.add(prepareJsonObject(student, false));
        }
        this.writeJSON(STUDENTS_JSON, updatedStudents);
    }

    private ArrayList<Course> parseAllCourses(JSONArray courseArray) {
        ArrayList<Course> courses = new ArrayList<>();
        Course course;
        for (Object obj : courseArray) {
            course = new Course();
            JSONObject jsonObj = (JSONObject) obj;
            JSONObject courseObj = (JSONObject) jsonObj.get("course");
            course.setId(Integer.parseInt((String) courseObj.get("ID")));
            course.setName((String) courseObj.get("name"));
            courses.add(course);
        }
        return courses;
    }

    private ArrayList<Student> parseAllStudents(JSONArray studentArray) {
        ArrayList<Student> students = new ArrayList<>();
        Student student;
        for (Object obj : studentArray) {
            student = new Student();
            JSONObject jsonObjWrapper = (JSONObject) obj;
            JSONObject studentObj = (JSONObject) jsonObjWrapper.get("student");
            student.setId(Integer.parseInt((String) studentObj.get("ID")));
            student.setFirstname((String) studentObj.get("firstname"));
            student.setLastname((String) studentObj.get("lastname"));
            student.setCourse((String) studentObj.get("course"));
            students.add(student);
        }
        return students;
    }

    DataAccessJSON() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Course> getAllCourses() {
        JSONArray courseList = this.loadExistingCourses();
        if (courseList.size() != 0) {
            ArrayList<Course> courses = this.parseAllCourses(courseList);
            for (Course course : courses) {
                course.setStudents(this.getStudentsByCourse(course));
            }
            return courses;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Course getCourse(int courseID) {
        Course course = new Course();
        JSONArray courseList = this.loadExistingCourses();
        if (courseList.size() != 0) {
            for (Object eachObj : courseList) {
                JSONObject jsonObj = (JSONObject) eachObj;
                JSONObject courseObj = (JSONObject) jsonObj.get("course");
                String ID = (String) courseObj.get("ID");
                if (courseID == Integer.parseInt(ID)) {
                    course.setId(courseID);
                    course.setName((String) courseObj.get("name"));
                    course.setStudents(this.getStudentsByCourse(course));
                    return course;
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCourse(Course course) {
        JSONArray courseList = this.loadExistingCourses();
        courseList.add(prepareJsonObject(course, true));
        this.writeJSON(COURSES_JSON, courseList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateCourse(Course course) {
        JSONArray courseList = this.loadExistingCourses();
        ArrayList<Course> courses = this.parseAllCourses(courseList);
        courseList.clear();
        for (Course courseObj : courses) {
            if (courseObj.getId() == course.getId())
                courseObj.setName(course.getName());
            courseList.add(prepareJsonObject(courseObj, false));
        }
        this.writeJSON(COURSES_JSON, courseList);

        if (course.getStudents().size() > 0) {
            for (Student student : course.getStudents())
                student.setCourse(course.getName());
            this.updateRelatedStudents(course);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeCourse(Course course) {
        JSONArray courseList = this.loadExistingCourses();
        ArrayList<Course> courses = this.parseAllCourses(courseList);
        courseList.clear();
        for (Course courseObj : courses) {
            if (course.getId() != courseObj.getId())
                courseList.add(prepareJsonObject(courseObj, false));
        }
        this.writeJSON(COURSES_JSON, courseList);

        if (course.getStudents().size() > 0) {
            for (Student student : course.getStudents())
                student.setCourse("");
            this.updateRelatedStudents(course);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Student> getAllStudents() {
        JSONArray studentList = this.loadExistingStudents();
        if (studentList.size() == 0)
            return null;
        return this.parseAllStudents(studentList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Student getStudent(int studentID) {
        Student student = new Student();
        JSONArray studentList = this.loadExistingStudents();
        if (studentList.size() != 0) {
            for (Object eachObj : studentList) {
                JSONObject jsonObjWrapper = (JSONObject) eachObj;
                JSONObject studentObj = (JSONObject) jsonObjWrapper.get("student");
                String ID = (String) studentObj.get("ID");
                if (studentID == Integer.parseInt(ID)) {
                    student.setId(studentID);
                    student.setFirstname((String) studentObj.get("firstname"));
                    student.setLastname((String) studentObj.get("lastname"));
                    student.setCourse((String) studentObj.get("course"));
                    return student;
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addStudent(Student student) {
        JSONArray studentList = this.loadExistingStudents();
        studentList.add(prepareJsonObject(student, true));
        this.writeJSON(STUDENTS_JSON, studentList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateStudent(Student student) {
        JSONArray studentList = this.loadExistingStudents();
        ArrayList<Student> students = this.parseAllStudents(studentList);
        studentList.clear();
        for (Student studentObj : students) {
            if (studentObj.getId() == student.getId()) {
                studentObj.setFirstname(student.getFirstname());
                studentObj.setLastname(student.getLastname());
                studentObj.setCourse(student.getCourse());
            }
            studentList.add(prepareJsonObject(studentObj, false));
        }
        this.writeJSON(STUDENTS_JSON, studentList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeStudent(Student student) {
        JSONArray studentList = this.loadExistingStudents();
        ArrayList<Student> students = this.parseAllStudents(studentList);
        studentList.clear();
        for (Student studentObj : students) {
            if (student.getId() != studentObj.getId())
                studentList.add(prepareJsonObject(student, false));
        }
        this.writeJSON(STUDENTS_JSON, studentList);
    }
}
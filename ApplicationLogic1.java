import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class ApplicationLogic1 implements iApplicationLogic {

    IDataAccess database;
    public ApplicationLogic1(IDataAccess database) {
        this.database = database;
    }

    @Override
    public ArrayList<Student> getAllStudents() {
        ArrayList<Student> allStudents = this.database.getAllStudents();
        allStudents = this.sortByLastName(allStudents);
        return allStudents;
    }

    @Override
    public ArrayList<Course> getAllCourses() {
        ArrayList<Course> allCourses = this.database.getAllCourses();
        //TODO: Sort Course by name
        return allCourses;
    }

    @Override
    public ArrayList<Student> getAllStudentsForCourse(Course course) {
        ArrayList<Student> allStudentsByCourse = this.database.getCourse(course.getId()).getStudents();
        allStudentsByCourse = this.sortByLastName(allStudentsByCourse);
        return allStudentsByCourse;
    }

    @Override
    public Student getStudent(int studentID) {
        Student student = this.database.getStudent(studentID);
        return student;
    }

    @Override
    public boolean createNewCourse(String courseName) {
        Course course = new Course(courseName);
        if(isNameTaken(courseName)){
            return false;
        }
        this.database.addCourse(course);
        return true;
    }

    @Override
    public void createNewStudent(String firstName, String lastName) {
        Student student = new Student(firstName, lastName);
        this.database.addStudent(student);
    }

    @Override
    public void updateStudent(Student student, String newFirstName, String newLastName) {
        student.setFirstname(newFirstName);
        student.setLastname(newLastName);
        this.database.updateStudent(student);
    }

    @Override
    public boolean updateCourse(Course course, String newCourseName) {
        if(isNameTaken(newCourseName)){
            return false;
        }
        course.setName(newCourseName);
        this.database.updateCourse(course);
        return true;
    }

    @Override
    public void deleteCourse(Course course) {
        this.database.removeCourse(course);
    }

    @Override
    public void deleteStudent(Student student) {
        this.database.removeStudent(student);
    }

    @Override
    public void addStudentToCourse(Student student, Course course) {
        student.setCourse(course.getName());
        this.database.updateStudent(student);
    }

    @Override
    public void removeStudentFromCourse(Student student) {
        this.database.removeStudent(student);
    }

    private ArrayList<Student> sortByLastName (ArrayList<Student> studentList) {
        Collections.sort(studentList, new Comparator<Student>() {
            public int compare(Student one, Student other) {
                return one.getLastname().compareTo(other.getLastname());
            }
        });
        return studentList;
    }

    private boolean isNameTaken (String newCourseName){
        ArrayList<Course> existingCourses = this.database.getAllCourses();
        for (Course x : existingCourses) {
            if(x.getName().equals(newCourseName))
            {
                return true;
            }
        }
        return false;
    }
}



package su26.uml.be.service.adminDashboard;

public interface ActivityTrackerService {
    void trackActivity(String userEmail);
    long getDailyActiveUsers();
    long getMonthlyActiveUsers();
}

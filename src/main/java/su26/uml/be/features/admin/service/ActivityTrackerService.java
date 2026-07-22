package su26.uml.be.features.admin.service;

public interface ActivityTrackerService {
    void trackActivity(String userEmail);
    long getDailyActiveUsers();
    long getMonthlyActiveUsers();
}
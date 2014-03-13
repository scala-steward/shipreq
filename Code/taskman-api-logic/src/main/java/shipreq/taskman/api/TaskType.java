package shipreq.taskman.api;

public enum TaskType {

    RegistrationRequested(100),
    RegistrationCompleted(101),
    PasswordResetRequested(102),
    LandingPageHit(200);

    public final int id;
    private TaskType(int id) {
        this.id = id;
    }
}

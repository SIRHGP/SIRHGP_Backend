package com.example.backend_sirhgp.event;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import org.springframework.context.ApplicationEvent;

public class TaskAssignedEvent extends ApplicationEvent {
    private final Tache task;
    private final Employer assignee;
    private final Employer assignedBy;

    public TaskAssignedEvent(Object source, Tache task, Employer assignee, Employer assignedBy) {
        super(source);
        this.task = task;
        this.assignee = assignee;
        this.assignedBy = assignedBy;
    }

    public Tache getTask() { return task; }
    public Employer getAssignee() { return assignee; }
    public Employer getAssignedBy() { return assignedBy; }
}
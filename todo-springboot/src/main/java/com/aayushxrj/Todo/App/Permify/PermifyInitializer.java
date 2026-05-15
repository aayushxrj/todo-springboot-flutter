package com.aayushxrj.Todo.App.Permify;

import com.aayushxrj.Todo.App.Task.TaskItem;
import com.aayushxrj.Todo.App.Task.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PermifyInitializer implements ApplicationRunner {

    private static final String SYSTEM_ROOT_ID = "root";

    private final PermifyClient permifyClient;
    private final TaskRepository taskRepository;
    private final Resource schemaResource;

    public PermifyInitializer(
            PermifyClient permifyClient,
            TaskRepository taskRepository,
            @Value("${permify.schema-path}") Resource schemaResource
    ) {
        this.permifyClient = permifyClient;
        this.taskRepository = taskRepository;
        this.schemaResource = schemaResource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String schema = new String(schemaResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        permifyClient.writeSchema(schema);

        List<Map<String, Object>> tuples = new ArrayList<>();
        tuples.add(systemRoleTuple("admin", "admin"));
        tuples.add(systemRoleTuple("user", "user"));

        for (TaskItem task : taskRepository.findAll()) {
            tuples.add(taskSystemTuple(task.getId().toString()));
        }

        permifyClient.writeTuples(tuples);
    }

    public Map<String, Object> taskSystemTuple(String taskId) {
        return Map.of(
                "entity", Map.of("type", "task", "id", taskId),
                "relation", "system",
                "subject", Map.of("type", "system", "id", SYSTEM_ROOT_ID, "relation", "")
        );
    }

    private Map<String, Object> systemRoleTuple(String role, String username) {
        return Map.of(
                "entity", Map.of("type", "system", "id", SYSTEM_ROOT_ID),
                "relation", role,
                "subject", Map.of("type", "user", "id", username, "relation", "")
        );
    }
}

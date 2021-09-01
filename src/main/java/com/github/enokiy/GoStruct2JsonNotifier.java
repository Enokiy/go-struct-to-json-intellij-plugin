package com.github.enokiy;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class GoStruct2JsonNotifier {
    private static NotificationGroup notificationGroup;
    static{
        notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("GoStruct2Json.NotificationGroup");
    }

    public static void notifyWarning(Project project, String msg) {
        notificationGroup.createNotification(msg, NotificationType.WARNING).notify(project);
    }

    public static void notifyInfo(Project project, String msg) {
        notificationGroup.createNotification(msg, NotificationType.INFORMATION).notify(project);
    }
    public static void notifyError(Project project, String msg) {
        notificationGroup.createNotification(msg, NotificationType.ERROR).notify(project);

    }
}

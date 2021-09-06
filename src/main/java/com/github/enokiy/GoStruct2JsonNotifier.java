package com.github.enokiy;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;

public class GoStruct2JsonNotifier {
    private static NotificationGroup notificationGroup;
    static{ // Compatible with older versions
        int buildNumber = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
        if (buildNumber <203){
            notificationGroup = new NotificationGroup("GoStruct2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
        }else{
            notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("GoStruct2Json.NotificationGroup");
        }
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

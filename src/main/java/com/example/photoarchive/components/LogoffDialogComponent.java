package com.example.photoarchive.components;

import com.vaadin.flow.component.html.Div;

public class LogoffDialogComponent extends Div {
    public LogoffDialogComponent(Runnable onConfirm) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setTitle(getTranslation("Are_you_sure_to_log_off?"));
        confirmDialog.setConfirmText(getTranslation("Confirm"));
        confirmDialog.addConfirmListener(e -> onConfirm.run());
        confirmDialog.setCancelText(getTranslation("Cancel"));
        confirmDialog.open();
    }
}

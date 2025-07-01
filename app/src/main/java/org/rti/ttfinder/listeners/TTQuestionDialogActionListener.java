package org.rti.ttfinder.listeners;

import org.rti.ttfinder.enums.Eye;

public interface TTQuestionDialogActionListener {
    void  onPressDone(String answer, String eye);
}
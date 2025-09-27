package org.newdawn.spaceinvaders.auth;

import org.newdawn.spaceinvaders.auth.FirebaseAuthClient;
import org.newdawn.spaceinvaders.auth.FirebaseConfig;
import org.newdawn.spaceinvaders.auth.UserSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class LoginDialog extends JDialog {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton signInButton;
    private JButton signUpButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    private UserSession session;

    public LoginDialog(Frame parent) {
        super(parent, "Login / Sign Up", true);
        buildUi();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }

    public UserSession showDialog() {
        setVisible(true);
        return session;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        form.add(new JLabel("Email"), gc);
        gc.gridx = 1;
        emailField = new JTextField(22);
        form.add(emailField, gc);

        gc.gridx = 0; gc.gridy++;
        form.add(new JLabel("Password"), gc);
        gc.gridx = 1;
        passwordField = new JPasswordField(22);
        form.add(passwordField, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        signInButton = new JButton(new AbstractAction("Sign In") {
            @Override public void actionPerformed(ActionEvent e) { doAuth(false); }
        });
        signUpButton = new JButton(new AbstractAction("Sign Up") {
            @Override public void actionPerformed(ActionEvent e) { doAuth(true); }
        });
        cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
        buttons.add(signUpButton);
        buttons.add(signInButton);
        buttons.add(cancelButton);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(60,60,60));

        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        root.add(statusLabel, BorderLayout.NORTH);
        setContentPane(root);
    }

    private void setBusy(boolean busy) {
        signInButton.setEnabled(!busy);
        signUpButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void doAuth(boolean signUp) {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter email and password.");
            statusLabel.setForeground(Color.RED);
            return;
        }
        setBusy(true);
        statusLabel.setText(signUp ? "Creating account..." : "Signing in...");
        statusLabel.setForeground(new Color(60,60,60));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try {
                    FirebaseAuthClient client = new FirebaseAuthClient(FirebaseConfig.WEB_API_KEY);
                    session = signUp ? client.signUp(email, password) : client.signIn(email, password);
                } catch (IOException | FirebaseAuthClient.FirebaseAuthException ex) {
                    error = ex;
                }
                return null;
            }
            @Override protected void done() {
                setBusy(false);
                if (error != null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Auth failed: " + error.getMessage());
                    session = null;
                } else {
                    statusLabel.setForeground(new Color(0,128,0));
                    statusLabel.setText("Success! Welcome" + (session.getEmail() != null ? (", " + session.getEmail()) : "") + ".");
                    Timer t = new Timer(500, e -> dispose());
                    t.setRepeats(false);
                    t.start();
                }
            }
        };
        worker.execute();
    }
}
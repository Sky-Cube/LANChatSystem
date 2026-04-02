package project.chat.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientChatFrame extends JFrame {

    private JTextPane chatPane;
    private JTextArea inputArea;
    private RoundedButton sendButton;
    private JList<String> userList;
    private JLabel userCountLabel;
    private JLabel userInfoLabel;

    private String loginTime;
    private Socket socket;
    private String nick;
    private String targetUser;

    public ClientChatFrame(String nick, Socket socket) {
        this.nick = nick;
        this.socket = socket;
        this.loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        setTitle("局域网聊天系统");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        setVisible(true);
        new ClientReaderThread(socket, this).start();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 聊天区
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(245, 245, 245));
        JScrollPane chatScroll = new JScrollPane(chatPane);

        // ================= 右侧 =================
        userCountLabel = new JLabel("在线人数: 0");
        userList = new JList<>();

        JButton privateButton = new JButton("私聊");
        privateButton.setBackground(new Color(255, 153, 0));
        privateButton.setForeground(Color.WHITE);

        privateButton.addActionListener(e -> {
            String selected = userList.getSelectedValue();

            if (selected == null) {
                JOptionPane.showMessageDialog(this, "请选择私聊对象");
                return;
            }

            if (selected.equals(targetUser)) {
                targetUser = null;
            } else {
                targetUser = selected;
            }

            updateModeLabel(); // 更新状态显示-显示当前聊天是私聊状态还是群聊状态
        });

        JPanel rightTop = new JPanel(new BorderLayout());
        rightTop.add(userCountLabel, BorderLayout.NORTH);
        rightTop.add(privateButton, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(rightTop, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(180, 0));

        // ================= 输入区 =================
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);

        sendButton = new RoundedButton("发送");
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.attachHoverListener();

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputScroll, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // 增加模式显示
        userInfoLabel = new JLabel();
        userInfoLabel.setForeground(Color.GRAY);
        updateModeLabel(); // 初始化

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomPanel, BorderLayout.CENTER);
        southPanel.add(userInfoLabel, BorderLayout.SOUTH);

        mainPanel.add(chatScroll, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);

        sendButton.addActionListener(e -> sendMessage());

        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "send");
        inputArea.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    // 更新聊天状态
    private void updateModeLabel() {
        String mode = (targetUser == null) ?
                "当前模式：群聊" :
                "当前模式：私聊 → " + targetUser;

        userInfoLabel.setText("用户: " + nick + " | " + mode + " | 登录时间: " + loginTime);
    }

    private void sendMessage() {
        String msg = inputArea.getText().trim();
        if (!msg.isEmpty()) {
            sendButton.animateClick();
            inputArea.setText("");

            try {
                if (targetUser != null && !targetUser.equals(nick)) {
                    sendPrivateMsgToClient(nick, targetUser, msg);
                    appendMessage("我->" + targetUser, msg, getTime(), true);
                } else {
                    sendMsgToClient(msg);
                    appendMessage("我", msg, getTime(), true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMsgToClient(String msg) throws IOException {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(2);
        dos.writeUTF(msg);
        dos.flush();
    }

    void sendPrivateMsgToClient(String nick, String targetUser, String msg) throws IOException {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(3);
        dos.writeUTF(nick);
        dos.writeUTF(targetUser);
        dos.writeUTF(msg);
        dos.flush();
    }

    private String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public void updateOnLineList(String[] onLineNames) {
        userList.setListData(onLineNames);
        userCountLabel.setText("在线人数: " + onLineNames.length);
    }

    public void setMsgToClient(String msg) {
        appendMessage("群聊", msg, getTime(), false);
    }

    public void setPrivateMsgToClient(String name, String targetUser, String privateMsg) {
        appendMessage(name + "->你", privateMsg, getTime(), false);
    }

    private void appendMessage(String user, String msg, String time, boolean isSelf) {
        try {
            Document doc = chatPane.getDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, isSelf ? new Color(0, 102, 204) : Color.BLACK);

            doc.insertString(doc.getLength(),
                    "[" + time + "] " + user + ": " + msg + "\n", style);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= 美化按钮 =================
    class RoundedButton extends JButton {
        private boolean hover = false;
        private boolean pressed = false;

        public RoundedButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFont(new Font("微软雅黑", Font.BOLD, 16));
            setPreferredSize(new Dimension(100, 45)); // 【更大更明显】
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c1 = hover ? new Color(120, 150, 255) : new Color(100, 133, 244);
            Color c2 = hover ? new Color(80, 110, 220) : new Color(70, 100, 210);

            if (pressed) {
                c1 = c1.darker();
                c2 = c2.darker();
            }

            GradientPaint gp = new GradientPaint(0, 0, c1, 0, getHeight(), c2);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
        }

        public void attachHoverListener() {
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        public void animateClick() {
            pressed = true;
            repaint();
            new Timer(100, e -> {
                pressed = false;
                repaint();
            }).start();
        }
    }
}
package project.chat.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class LoginFrame extends JFrame {

    private RoundedTextField nicknameField;
    private JButton loginButton;
    private JButton exitButton;
    private Socket socket;//定义socket全局变量，使其能调用传送给聊天页面

    // 主题颜色
    private static final Color PRIMARY_COLOR = new Color(79, 115, 255);
    private static final Color ERROR_COLOR = new Color(235, 87, 87);
    private static final Color BG_COLOR = new Color(240, 242, 245);
    private static final Color CARD_COLOR = Color.WHITE;
    //强制字体
    private static final Font CHINESE_FONT = new Font("Dialog", Font.PLAIN, 13);
    private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 28);
    private static final Font BUTTON_FONT = new Font("Dialog", Font.BOLD, 15);

    public LoginFrame() {
        initUI();
    }

    //页面主要功能
    private void initUI() {
        setTitle("局域网聊天室");
        setSize(480, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG_COLOR);

        JPanel cardPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int shadowSize = 8;
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(shadowSize / 2, shadowSize / 2,
                        getWidth() - shadowSize, getHeight() - shadowSize,
                        24, 24);

                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };

        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(new EmptyBorder(32, 32, 32, 32));

        // 图标
        JLabel iconLabel = new JLabel("💬", JLabel.CENTER);
        // 使用通用字体，避免 Emoji 在某些旧系统乱码，如果系统支持通常会正常显示
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 42));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("欢迎加入", JLabel.CENTER);
        titleLabel.setFont(TITLE_FONT); // 使用修复后的字体
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel("局域网即时通讯", JLabel.CENTER);
        subTitle.setFont(CHINESE_FONT); // 使用修复后的字体
        subTitle.setForeground(new Color(150, 150, 150));
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(iconLabel);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(subTitle);
        cardPanel.add(Box.createVerticalStrut(36));

        nicknameField = new RoundedTextField("请输入您的昵称", 16);
        nicknameField.setMaximumSize(new Dimension(320, 48));
        nicknameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(nicknameField);

        cardPanel.add(Box.createVerticalStrut(32));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        loginButton = createGradientButton("登录", PRIMARY_COLOR);
        exitButton = createGradientButton("退出", ERROR_COLOR);

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        cardPanel.add(buttonPanel);

        cardPanel.add(Box.createVerticalGlue());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_COLOR);
        contentPanel.add(cardPanel, BorderLayout.CENTER);
        setContentPane(contentPanel);

        setupEvents();

        setVisible(true);
        nicknameField.requestFocusInWindow();
    }
    //添加按钮动画效果
    private JButton createGradientButton(String text, Color baseColor) {
        GradientButton btn = new GradientButton(text, baseColor);
        //添加鼠标监听器
        btn.addMouseListener(new MouseAdapter() {
            //鼠标移入事件 触发时机: 当鼠标指针进入按钮区域时触发
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.animateHover(true);
            }
            //鼠标移出实践 触发时机: 当鼠标指针离开按钮区域时触发
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.animateHover(false);
            }
        });

        return btn;
    }

    private void setupEvents() {
        nicknameField.addActionListener(e -> {
            try {
                performLogin();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        loginButton.addActionListener(e -> performLogin());


        exitButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "确定退出？");
            if (result == JOptionPane.YES_OPTION) System.exit(0);
        });
    }
    //登录校验逻辑。它的作用是在用户点击“登录”时，先检查输入框里有没有填昵称。如果没填，就弹窗提示错误；如果填了，就弹窗表示欢迎
    // 登录逻辑
    private void performLogin() {
        String nick = nicknameField.getText();

        if (nick == null || nick.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入有效昵称！");
            return;
        }

        // 防止重复登录（关键）
        if (socket != null && socket.isConnected()) {
            JOptionPane.showMessageDialog(this, "已经登录，请勿重复点击！");
            return;
        }

        try {
            login(nick);
            System.out.println("准备打开聊天窗口");
            //创建一个新的聊天页面窗口对象
            new ClientChatFrame(nick, socket);

            this.dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "登录失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    //创建登录方法 处理socket
    private void login(String nick) throws Exception {
        System.out.println("开始连接服务器...");
        if (socket == null || socket.isClosed()) {
            socket = new Socket(Constant.SERVER_IP, Constant.Client_port);
        }

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(1);
        dos.writeUTF(nick);
        dos.flush();
    }
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.font", BUTTON_FONT);
            UIManager.put("Label.font", CHINESE_FONT);
            UIManager.put("TextField.font", CHINESE_FONT);
            UIManager.put("TextArea.font", CHINESE_FONT);
            UIManager.put("OptionPane.messageFont", CHINESE_FONT);
            UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
            UIManager.put("Menu.font", CHINESE_FONT);

        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(LoginFrame::new);
    }

    // 按钮内部类  以下是按钮的各种效果
    class GradientButton extends JButton {
        private float alpha = 1.0f;
        private Timer hoverTimer;
        private Color baseColor;

        public GradientButton(String text, Color baseColor) {
            super(text);
            this.baseColor = baseColor;

            setUI(new BasicButtonUI());
            setOpaque(false);
            setBorder(null);
            setFocusPainted(false);

            setFont(BUTTON_FONT); // 直接使用常量
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(110, 44));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(alpha * 255)),
                    0, getHeight(), new Color(baseColor.darker().getRed(), baseColor.darker().getGreen(), baseColor.darker().getBlue(), (int)(alpha * 255))
            );

            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, y);
            g2.dispose();
        }

        public void animateHover(boolean enter) {
            if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();

            float target = enter ? 0.85f : 1.0f;
            float step = (target - alpha) / 10;

            hoverTimer = new Timer(10, e -> {
                if ((target > alpha && alpha + step >= target) ||
                        (target < alpha && alpha + step <= target)) {
                    alpha = target;
                    hoverTimer.stop();
                } else {
                    alpha += step;
                }
                repaint();
            });
            hoverTimer.start();
        }
    }

    //输入框内部类
    class RoundedTextField extends JTextField {
        private final String placeholder;
        private boolean showPlaceholder = true;
        private Color borderColor = new Color(220, 220, 220);
        private final int arc = 12;

        public RoundedTextField(String placeholder, int fontSize) {
            this.placeholder = placeholder;

            setFont(new Font("Dialog", Font.PLAIN, fontSize)); // 使用通用字体
            setForeground(new Color(160, 160, 160));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

            setText(placeholder);

            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (showPlaceholder) {
                        setText("");
                        setForeground(Color.BLACK);
                        showPlaceholder = false;
                    }
                    borderColor = PRIMARY_COLOR;
                    repaint();
                }

                public void focusLost(FocusEvent e) {
                    if (getText().isEmpty()) {
                        setText(placeholder);
                        setForeground(new Color(160, 160, 160));
                        showPlaceholder = true;
                    }
                    borderColor = new Color(220, 220, 220);
                    repaint();
                }
            });
        }

        public String getText() {
            return showPlaceholder ? "" : super.getText();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
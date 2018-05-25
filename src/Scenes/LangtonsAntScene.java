import java.util.*;

import flatlab.*;
import flatlab.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

public class LangtonsAntScene extends Scene {

    enum Direction {
        Up,
        Down,
        Left,
        Right,
    }

    class Ant {
        public int x;
        public int y;
        public Direction d;
        public Color c;

        public Ant(int x, int y, Direction d, Color c) {
            this.x = x;
            this.y = y;
            this.d = d;
            this.c = c;
        }
    }

    class Color {
        public float r = 0f;
        public float g = 0f;
        public float b = 0f;

        public Color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static final int SCREEN_WIDTH = 1000;
    public static final int SCREEN_HEIGHT = 800;

    private final int WORLD_SIZE_X = 500;
    private final int WORLD_SIZE_Y = 400;

    private int _tile_size = 64;
    private int _tile_textureid;

    private float _cam_x = 0;
    private float _cam_y = 0;
    private float _cam_speed = 0.5f;
    private float _cam_x_wanted = (WORLD_SIZE_X / 2 - 5) * (_tile_size);
    private float _cam_y_wanted = (WORLD_SIZE_Y / 2 - 5) * (_tile_size);

    private float _cam_zoom = 0.1f;
    private float _cam_zoom_wanted = 0.05f;
    private float _cam_zoom_increment = 0.15f; // 5%
    private float _cam_zoom_speed = 0.05f;

    Random rng = new Random(42);

    boolean[][] world = new boolean[WORLD_SIZE_X][WORLD_SIZE_Y];
    Color[][] colormap = new Color[WORLD_SIZE_X][WORLD_SIZE_Y];

    boolean running = false;

    List<Ant> ants = new ArrayList();

    public LangtonsAntScene() {
        super(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    public void mouse_scroll(float x, float y) {
        if(y > 0)
            _cam_zoom_wanted *= 1 + _cam_zoom_increment;
        else
            if (_cam_zoom_wanted > 0f)
                _cam_zoom_wanted *= 1 - _cam_zoom_increment;
    }

    public void key(int key, int action, int mods) {
        if (key == 'A') _cam_x_wanted -= 10 * (1 / _cam_zoom);
        if (key == 'D') _cam_x_wanted += 10 * (1 / _cam_zoom);
        if (key == 'W') _cam_y_wanted -= 10 * (1 / _cam_zoom);
        if (key == 'S') _cam_y_wanted += 10 * (1 / _cam_zoom);
        if (key == ',') {
            speed *= 0.9f;
            System.out.println("speed: " + speed);
        }
        if (key == '.') {
            speed *= 1.1f;
            System.out.println("speed: " + speed);
        }
        if (key == ' ' && action == 0) {
            running = !running;
            System.out.println("running: " + running);
        }
    }

    public void init() {
        _tile_textureid = TextureLoader.load("res/square512.png");

        for(int i = 0; i < WORLD_SIZE_X; i++) {
            for(int j = 0; j < WORLD_SIZE_Y; j++) {
                colormap[i][j] = new Color(0.247f,0.247f,0.247f);
            }
        }

        int new_ants = rng.nextInt(500)+200;
        for(int i = 0; i < new_ants; i++) {
            ants.add(new Ant(rng.nextInt(WORLD_SIZE_X)-1,
                             rng.nextInt(WORLD_SIZE_Y)-1,
                             Direction.values()[rng.nextInt(4)],
                             new Color(rng.nextFloat(), rng.nextFloat(), rng.nextFloat())));
        }
    }

    float acc = 0;
    float speed = 10f;
    public void update(float dt) {
        if (_cam_zoom != _cam_zoom_wanted)
            _cam_zoom = Flatmath.lerp(_cam_zoom, _cam_zoom_wanted, _cam_zoom_speed);

        if (_cam_x_wanted != _cam_x)
            _cam_x = Flatmath.lerp(_cam_x, _cam_x_wanted, _cam_speed);

        if (_cam_y_wanted != _cam_y)
            _cam_y = Flatmath.lerp(_cam_y, _cam_y_wanted, _cam_speed);

        acc += dt;
        if(acc > speed) {
            acc = 0;

            if (running) {
                for(Ant a : ants) {
                    if(world[(int)a.x][(int)a.y]) {
                        world[(int)a.x][(int)a.y] = false;

                        switch(a.d) {
                        case Up:
                            a.x -= 1;
                            a.d = Direction.Left;
                            break;
                        case Down:
                            a.x += 1;
                            a.d = Direction.Right;
                            break;
                        case Left:
                            a.y += 1;
                            a.d = Direction.Down;
                            break;
                        case Right:
                            a.y -= 1;
                            a.d = Direction.Up;
                            break;
                        }
                    } else {
                        world[(int)a.x][(int)a.y] = true;

                        switch(a.d) {
                        case Up:
                            a.x += 1;
                            a.d = Direction.Right;
                            break;
                        case Down:
                            a.x -= 1;
                            a.d = Direction.Left;
                            break;
                        case Left:
                            a.y -= 1;
                            a.d = Direction.Up;
                            break;
                        case Right:
                            a.y += 1;
                            a.d = Direction.Down;
                            break;
                        }
                    }

                    a.x = (int)Flatmath.clamp(0, WORLD_SIZE_X-1, a.x);
                    a.y = (int)Flatmath.clamp(0, WORLD_SIZE_Y-1, a.y);
                    colormap[a.x][a.y] = a.c;
                }
            }
        }
    }

    private void draw_tiles() {
        float world_width = (SCREEN_WIDTH * (1 / _cam_zoom));
        float world_height = (SCREEN_HEIGHT * (1 / _cam_zoom));

        float min_x = _cam_x + (SCREEN_WIDTH / 2) - (world_width / 2);
        float max_x = _cam_x + (SCREEN_WIDTH / 2) + (world_width / 2);

        float min_y = _cam_y + (SCREEN_HEIGHT / 2) - (world_height / 2);
        float max_y = _cam_y + (SCREEN_HEIGHT / 2) + (world_height / 2);

        int tile_padding = 2;

        int start_x = (int)(min_x / _tile_size) - tile_padding;
        int end_x = (int)(max_x / _tile_size) + tile_padding;

        int start_y = (int)(min_y / _tile_size) - tile_padding;
        int end_y = (int)(max_y / _tile_size) + tile_padding;

        glColor3f(0.941f, 0.875f, 0.686f); // zenburn yellow

        glBindTexture(GL_TEXTURE_2D, _tile_textureid);
        glBegin(GL_QUADS);

        int spacing_x = _tile_size;
        int spacing_y = _tile_size;

        for(int i = start_x; i < end_x; i++) {
            for(int j = start_y; j < end_y; j++) {
                // dont draw tiles outside the bounds of our map
                if(i < 0 || j < 0 || i >= WORLD_SIZE_X || j >= WORLD_SIZE_Y)
                    continue;

                if(world[i][j]){ // if alive
                    // glColor3f(0.941f, 0.875f, 0.686f);
                    glColor3f(colormap[i][j].r, colormap[i][j].g, colormap[i][j].b);
                }
                else {
                    glColor3f(0.247f, 0.247f, 0.247f);
                }

                glTexCoord2f(0, 0); // top left
                glVertex2f(i * spacing_x, j * spacing_y);
                glTexCoord2f(0, 1); // bottom left
                glVertex2f(i * spacing_x, j * spacing_y + _tile_size);
                glTexCoord2f(1, 1); // bottom right
                glVertex2f(i * spacing_x + _tile_size, j * spacing_y + _tile_size);
                glTexCoord2f(1, 0); // top right
                glVertex2f(i * spacing_x + _tile_size, j * spacing_y);
            }
        }

        for(Ant a : ants) {
            glColor3f(1,0,0);

            glTexCoord2f(0, 0); // top left
            glVertex2f(a.x * spacing_x, a.y * spacing_y);
            glTexCoord2f(0, 1); // bottom left
            glVertex2f(a.x * spacing_x, a.y * spacing_y + _tile_size);
            glTexCoord2f(1, 1); // bottom right
            glVertex2f(a.x * spacing_x + _tile_size, a.y * spacing_y + _tile_size);
            glTexCoord2f(1, 0); // top right
            glVertex2f(a.x * spacing_x + _tile_size, a.y * spacing_y);
        }

        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void draw() {
        glPushMatrix();

        // transform to the camera
        glTranslatef(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2, 0);
        glScalef(_cam_zoom, _cam_zoom, 1);
        glTranslatef(-(SCREEN_WIDTH / 2), -(SCREEN_HEIGHT / 2), 0);

        glTranslatef(-_cam_x, -_cam_y, 0);

        draw_tiles();

        glPopMatrix();
    }
}

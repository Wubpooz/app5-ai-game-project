import numpy as np

from manim import (
    ApplyWave,
    Arrow,
    BOLD,
    Circle,
    Create,
    CurvedArrow,
    DOWN,
    DL,
    FadeIn,
    FadeOut,
    GrowArrow,
    Indicate,
    LEFT,
    LaggedStart,
    MathTex,
    ORIGIN,
    PI,
    RIGHT,
    RegularPolygon,
    RoundedRectangle,
    Scene,
    Square,
    Star,
    SurroundingRectangle,
    Text,
    TransformFromCopy,
    UP,
    UL,
    VGroup,
    WHITE,
    Write,
    config,
)


config.background_color = "#08111F"
config.frame_rate = 30


BOARD_BANDS = [
    [3, 2, 2, 1, 3, 2],
    [1, 3, 1, 3, 1, 2],
    [2, 1, 3, 2, 3, 1],
    [2, 3, 1, 2, 1, 3],
    [3, 1, 3, 1, 3, 2],
    [1, 2, 2, 3, 1, 2],
]

COLORS = {
    "bg": "#08111F",
    "panel": "#0F1A2F",
    "panel_soft": "#13213A",
    "grid": "#22304A",
    "text": "#F4F7FB",
    "muted": "#9EB0C9",
    "accent": "#78D7FF",
    "accent2": "#FFC65A",
    "accent3": "#FF8A7A",
    "band1": "#385D8A",
    "band2": "#4E8E6B",
    "band3": "#B5743D",
    "me": "#5DE4B6",
    "opp": "#FF6F91",
    "pass": "#F55B5B",
    "shadow": "#050A13",
}


def make_title_block(title, subtitle):
    title_mob = Text(title, font_size=44, weight=BOLD, color=COLORS["text"])
    subtitle_mob = Text(subtitle, font_size=22, color=COLORS["muted"])
    subtitle_mob.next_to(title_mob, DOWN, buff=0.25)
    return VGroup(title_mob, subtitle_mob)


def make_chip(label, fill_color, text_color=WHITE):
    chip_text = Text(label, font_size=18, color=text_color, weight=BOLD)
    chip = RoundedRectangle(
        corner_radius=0.18,
        width=chip_text.width + 0.55,
        height=chip_text.height + 0.28,
        stroke_width=0,
        fill_color=fill_color,
        fill_opacity=0.96,
    )
    chip_text.move_to(chip.get_center())
    return VGroup(chip, chip_text)


def make_metric_pill(label, value, accent_color):
    text = Text(f"{label}: {value}", font_size=18, color=COLORS["text"], weight=BOLD)
    pill = RoundedRectangle(
        corner_radius=0.2,
        width=text.width + 0.6,
        height=text.height + 0.28,
        stroke_width=1.5,
        stroke_color=accent_color,
        fill_color=COLORS["panel_soft"],
        fill_opacity=1,
    )
    text.move_to(pill.get_center())
    return VGroup(pill, text)


def make_board(scale=1.0, show_indices=False):
    size = 0.72 * scale
    board = VGroup()
    top_left = np.array([-2.1 * scale, 2.1 * scale, 0.0])

    for row in range(6):
        for col in range(6):
            band = BOARD_BANDS[row][col]
            color = {1: COLORS["band1"], 2: COLORS["band2"], 3: COLORS["band3"]}[band]
            square = Square(side_length=size, stroke_width=2, stroke_color=COLORS["grid"])
            square.set_fill(color, opacity=0.86)
            square.move_to(top_left + np.array([col * size, -row * size, 0.0]))
            board.add(square)

            tag = Text(str(band), font_size=18, color=WHITE, weight=BOLD)
            tag.set_opacity(0.42)
            tag.move_to(square.get_center())
            board.add(tag)

            if show_indices:
                idx = row * 6 + col
                idx_text = Text(str(idx), font_size=11, color=WHITE)
                idx_text.set_opacity(0.25)
                idx_text.move_to(square.get_corner(DL) + np.array([0.14, 0.14, 0]))
                board.add(idx_text)

    border = RoundedRectangle(
        corner_radius=0.15,
        width=6 * size + 0.35,
        height=6 * size + 0.35,
        stroke_color=COLORS["accent"],
        stroke_width=2.5,
        fill_opacity=0,
    )
    border.move_to(board.get_center())
    board.add_to_back(border)
    return board


def board_square_center(row, col, scale=1.0):
    size = 0.72 * scale
    top_left = np.array([-2.1 * scale, 2.1 * scale, 0.0])
    return top_left + np.array([col * size, -row * size, 0.0])


def make_piece(symbol, color, position, scale=1.0, kind="circle"):
    if kind == "triangle":
        piece = RegularPolygon(n=3, start_angle=PI / 2)
    elif kind == "star":
        piece = Star(n=5)
    else:
        piece = Circle()
    piece.set_height(0.34 * scale)
    piece.set_width(0.34 * scale)
    piece.set_fill(color, opacity=1)
    piece.set_stroke(WHITE, width=2)
    piece.move_to(position)
    label = Text(symbol, font_size=int(18 * scale), weight=BOLD, color=COLORS["bg"])
    label.move_to(piece.get_center())
    return VGroup(piece, label)


def make_channel_stack(title):
    frame = RoundedRectangle(
        corner_radius=0.18,
        width=2.5,
        height=4.85,
        stroke_color=COLORS["grid"],
        stroke_width=2,
        fill_color=COLORS["panel"],
        fill_opacity=1,
    )
    header = Text(title, font_size=22, color=COLORS["text"], weight=BOLD)
    header.next_to(frame, UP, buff=0.2)

    rows = VGroup()
    row_specs = [
        ("0-3", "pieces", COLORS["me"]),
        ("4-6", "bands", COLORS["accent"]),
        ("7-10", "tempo", COLORS["accent2"]),
        ("11-14", "stats", COLORS["muted"]),
        ("15", "unicorn-rel", COLORS["accent3"]),
    ]

    y_positions = [1.75, 0.85, -0.05, -0.95, -1.85]
    for (span, label, color), y in zip(row_specs, y_positions):
        pill = RoundedRectangle(
            corner_radius=0.12,
            width=2.1,
            height=0.66,
            stroke_width=1.2,
            stroke_color=color,
            fill_color=color,
            fill_opacity=0.17,
        )
        pill.move_to(frame.get_center() + np.array([0, y, 0]))
        span_text = Text(span, font_size=16, color=color, weight=BOLD)
        span_text.next_to(pill, LEFT, buff=0.16)
        label_text = Text(label, font_size=16, color=COLORS["text"])
        label_text.move_to(pill.get_center())
        rows.add(VGroup(pill, span_text, label_text))

    stack = VGroup(frame, rows, header)
    return stack


class BandDPERExplainer(Scene):
    def construct(self):
        title = make_title_block("BandDPER", "Band-aware dual-perspective evaluation network for Escampe")
        title.to_edge(UP, buff=0.45)

        thesis = Text(
            "The model turns a small, irregular board into a bounded evaluation in [-1, +1].",
            font_size=24,
            color=COLORS["muted"],
        )
        thesis.next_to(title, DOWN, buff=0.3)

        chips = VGroup(
            make_chip("Siamese encoder", COLORS["accent"]),
            make_chip("Residual trunk", COLORS["band2"]),
            make_chip("Forced-pass shortcut", COLORS["pass"]),
        ).arrange(RIGHT, buff=0.25)
        chips.next_to(thesis, DOWN, buff=0.4)

        band_board = make_board(scale=0.88)
        band_board.to_edge(LEFT, buff=0.5)
        board_label = Text("Fixed band topology", font_size=20, color=COLORS["text"], weight=BOLD)
        board_label.next_to(band_board, UP, buff=0.2)
        board_note = Text("The same 6x6 map drives every perspective.", font_size=18, color=COLORS["muted"])
        board_note.next_to(board_label, DOWN, buff=0.12)

        callout = VGroup(
            make_metric_pill("mobility", "asymmetry", COLORS["accent"]),
            make_metric_pill("tempo", "forced reply", COLORS["accent2"]),
            make_metric_pill("value", "tanh bounded", COLORS["accent3"]),
        ).arrange(DOWN, buff=0.22)
        callout.to_edge(RIGHT, buff=0.55)

        self.play(Write(title[0]), FadeIn(title[1], shift=UP * 0.2))
        self.play(FadeIn(thesis, shift=UP * 0.15))
        self.play(LaggedStart(*[FadeIn(chip, shift=UP * 0.15) for chip in chips], lag_ratio=0.12))
        self.play(Create(band_board), FadeIn(board_label, shift=UP * 0.12), FadeIn(board_note, shift=UP * 0.08))
        self.play(LaggedStart(*[FadeIn(pill, shift=RIGHT * 0.12) for pill in callout], lag_ratio=0.12))

        me_pieces = VGroup(
            make_piece("U", COLORS["me"], board_square_center(4, 1, 0.88), 0.88, kind="star"),
            make_piece("P", COLORS["me"], board_square_center(3, 0, 0.88), 0.88),
            make_piece("P", COLORS["me"], board_square_center(2, 1, 0.88), 0.88),
        )
        opp_pieces = VGroup(
            make_piece("U", COLORS["opp"], board_square_center(1, 4, 0.88), 0.88, kind="triangle"),
            make_piece("P", COLORS["opp"], board_square_center(0, 3, 0.88), 0.88),
            make_piece("P", COLORS["opp"], board_square_center(2, 4, 0.88), 0.88),
        )
        self.play(LaggedStart(*[FadeIn(m, scale=0.65) for m in me_pieces], lag_ratio=0.12))
        self.play(LaggedStart(*[FadeIn(m, scale=0.65) for m in opp_pieces], lag_ratio=0.12))

        highlights = VGroup(
            SurroundingRectangle(me_pieces[0], buff=0.08, color=COLORS["me"], stroke_width=2.5),
            SurroundingRectangle(opp_pieces[0], buff=0.08, color=COLORS["opp"], stroke_width=2.5),
        )
        self.play(Create(highlights[0]), Create(highlights[1]))

        self.play(
            ApplyWave(band_board[0:36], amplitude=0.06, time_width=0.6),
            run_time=1.8,
        )
        self.play(
            Indicate(callout[0], color=COLORS["accent"]),
            Indicate(callout[1], color=COLORS["accent2"]),
            Indicate(callout[2], color=COLORS["accent3"]),
            run_time=1.6,
        )

        self.wait(0.8)
        self.play(
            FadeOut(VGroup(title, thesis, chips, board_label, board_note, callout, highlights, me_pieces, opp_pieces), shift=DOWN * 0.12),
            band_board.animate.scale(0.92).move_to(ORIGIN),
            run_time=1.2,
        )


class BandDPERInputEncoding(Scene):
    def construct(self):
        heading = Text("1. Dual-perspective input encoding", font_size=34, color=COLORS["text"], weight=BOLD)
        heading.to_edge(UP, buff=0.4)
        sub = Text(
            "The same encoder reads current-player and opponent viewpoints with shared weights.",
            font_size=20,
            color=COLORS["muted"],
        )
        sub.next_to(heading, DOWN, buff=0.18)

        board = make_board(scale=0.72, show_indices=False)
        board.to_edge(LEFT, buff=0.35).shift(DOWN * 0.15)
        board_caption = Text("Board state", font_size=20, color=COLORS["text"], weight=BOLD)
        board_caption.next_to(board, UP, buff=0.15)

        x_me = make_channel_stack("x_me")
        x_opp = make_channel_stack("x_opp")
        x_me.to_edge(RIGHT, buff=0.3).shift(UP * 0.65 + LEFT * 0.15)
        x_opp.next_to(x_me, LEFT, buff=0.65)

        encoder = RoundedRectangle(
            corner_radius=0.2,
            width=2.55,
            height=2.2,
            stroke_color=COLORS["accent"],
            stroke_width=2.5,
            fill_color=COLORS["panel_soft"],
            fill_opacity=1,
        )
        encoder.move_to(DOWN * 1.55 + RIGHT * 0.45)
        enc_text = Text("Shared spatial\nencoder", font_size=24, color=COLORS["text"], weight=BOLD)
        enc_text.move_to(encoder.get_center())
        enc_note = Text("Conv -> dilated conv -> projection", font_size=16, color=COLORS["muted"])
        enc_note.next_to(encoder, DOWN, buff=0.15)

        siamese = RoundedRectangle(
            corner_radius=0.18,
            width=2.1,
            height=0.56,
            stroke_color=COLORS["accent2"],
            stroke_width=1.8,
            fill_color=COLORS["accent2"],
            fill_opacity=0.16,
        )
        siamese.move_to(UP * 1.25 + RIGHT * 0.45)
        siamese_text = Text("shared weights", font_size=18, color=COLORS["accent2"], weight=BOLD)
        siamese_text.move_to(siamese.get_center())

        arrow_me = Arrow(board.get_right() + RIGHT * 0.1, x_me.get_left() + LEFT * 0.15, buff=0.1, color=COLORS["me"], stroke_width=4)
        arrow_opp = Arrow(board.get_right() + RIGHT * 0.1, x_opp.get_left() + LEFT * 0.15, buff=0.1, color=COLORS["opp"], stroke_width=4)
        arrow_me.shift(UP * 0.8)
        arrow_opp.shift(DOWN * 0.55)

        fuse = RoundedRectangle(
            corner_radius=0.18,
            width=2.85,
            height=0.92,
            stroke_color=COLORS["accent3"],
            stroke_width=2.0,
            fill_color=COLORS["panel_soft"],
            fill_opacity=1,
        )
        fuse.move_to(RIGHT * 3.05 + DOWN * 0.1)
        fuse_text = Text("Concatenate\n[128, 128, 2] -> h", font_size=20, color=COLORS["text"], weight=BOLD)
        fuse_text.move_to(fuse.get_center())

        escape_pills = VGroup(
            make_metric_pill("escape_me", "0..16", COLORS["me"]),
            make_metric_pill("escape_opp", "0..16", COLORS["opp"]),
        ).arrange(DOWN, buff=0.15)
        escape_pills.next_to(fuse, DOWN, buff=0.25)

        band_channels = VGroup(
            make_chip("0-3 pieces", COLORS["me"]),
            make_chip("4-6 band masks", COLORS["accent"]),
            make_chip("7-10 landing maps", COLORS["accent2"]),
            make_chip("11-14 row/col stats", COLORS["muted"]),
            make_chip("15 unicorn-relative", COLORS["accent3"]),
        ).arrange(DOWN, buff=0.12, aligned_edge=LEFT)
        band_channels.next_to(x_opp, DOWN, buff=0.25).align_to(x_opp, LEFT)

        self.play(FadeIn(heading, shift=UP * 0.15), FadeIn(sub, shift=UP * 0.15))
        self.play(Create(board), FadeIn(board_caption, shift=UP * 0.08))
        self.play(FadeIn(x_opp, shift=RIGHT * 0.1), FadeIn(x_me, shift=RIGHT * 0.1))
        self.play(Create(arrow_me), Create(arrow_opp))
        self.play(FadeIn(siamese, shift=DOWN * 0.1), FadeIn(siamese_text, shift=DOWN * 0.1))
        self.play(FadeIn(encoder, shift=DOWN * 0.15), FadeIn(enc_text, shift=DOWN * 0.15), FadeIn(enc_note, shift=DOWN * 0.15))
        self.play(TransformFromCopy(x_me, fuse), TransformFromCopy(x_opp, fuse))
        self.play(FadeIn(fuse_text, shift=UP * 0.1))
        self.play(LaggedStart(*[FadeIn(p, shift=DOWN * 0.08) for p in escape_pills], lag_ratio=0.15))
        self.play(LaggedStart(*[FadeIn(chip, shift=DOWN * 0.08) for chip in band_channels], lag_ratio=0.07))

        self.play(
            Indicate(band_channels[0], color=COLORS["me"]),
            Indicate(band_channels[1], color=COLORS["accent"]),
            Indicate(band_channels[2], color=COLORS["accent2"]),
            Indicate(band_channels[4], color=COLORS["accent3"]),
            run_time=1.5,
        )

        self.wait(0.8)
        self.play(FadeOut(VGroup(heading, sub, board, board_caption, x_me, x_opp, band_channels, arrow_me, arrow_opp, siamese, siamese_text, encoder, enc_text, enc_note, fuse, fuse_text, escape_pills), shift=DOWN * 0.12))


class BandDPERHead(Scene):
    def construct(self):
        heading = Text("2. Residual trunk and shortcut head", font_size=34, color=COLORS["text"], weight=BOLD)
        heading.to_edge(UP, buff=0.4)
        sub = Text(
            "Escape counts enter the trunk, while forced-pass skips straight to the output.",
            font_size=20,
            color=COLORS["muted"],
        )
        sub.next_to(heading, DOWN, buff=0.18)

        h_box = RoundedRectangle(
            corner_radius=0.18,
            width=1.55,
            height=0.8,
            stroke_color=COLORS["accent"],
            stroke_width=2.2,
            fill_color=COLORS["panel_soft"],
            fill_opacity=1,
        )
        h_box.move_to(LEFT * 5.2 + DOWN * 0.35)
        h_text = Text("h\n[258]", font_size=22, color=COLORS["text"], weight=BOLD)
        h_text.move_to(h_box.get_center())

        esc_left = make_metric_pill("escape_me", "global", COLORS["me"])
        esc_right = make_metric_pill("escape_opp", "global", COLORS["opp"])
        esc_group = VGroup(esc_left, esc_right).arrange(DOWN, buff=0.16)
        esc_group.next_to(h_box, UP, buff=0.35).align_to(h_box, LEFT)

        trunk_boxes = VGroup()
        trunk_arrows = VGroup()
        for idx, color in enumerate([COLORS["accent"], COLORS["band2"], COLORS["accent2"]], start=1):
            block = RoundedRectangle(
                corner_radius=0.18,
                width=2.0,
                height=0.98,
                stroke_color=color,
                stroke_width=2.2,
                fill_color=COLORS["panel"],
                fill_opacity=1,
            )
            block.move_to(np.array([-1.85 + (idx - 1) * 2.45, -0.35, 0.0]))
            block_text = Text(f"ResBlock {idx}\n258 -> 258", font_size=20, color=COLORS["text"], weight=BOLD)
            block_text.move_to(block.get_center())
            trunk_boxes.add(VGroup(block, block_text))

        for idx in range(3):
            if idx == 0:
                start = h_box.get_right() + RIGHT * 0.12
            else:
                start = trunk_boxes[idx - 1][0].get_right() + RIGHT * 0.12
            end = trunk_boxes[idx][0].get_left() + LEFT * 0.12
            trunk_arrows.add(Arrow(start, end, buff=0.05, color=COLORS["grid"], stroke_width=4))

        skip_arrows = VGroup()
        for block in trunk_boxes:
            skip = CurvedArrow(
                block[0].get_top() + UP * 0.1,
                block[0].get_bottom() + DOWN * 0.1,
                angle=-PI / 2,
                color=COLORS["accent3"],
                stroke_width=3,
                tip_length=0.18,
            )
            skip_arrows.add(skip)

        shortcut = RoundedRectangle(
            corner_radius=0.18,
            width=1.7,
            height=0.7,
            stroke_color=COLORS["pass"],
            stroke_width=2.2,
            fill_color=COLORS["pass"],
            fill_opacity=0.16,
        )
        shortcut.move_to(LEFT * 5.05 + UP * 1.45)
        shortcut_text = Text("forced_pass", font_size=19, color=COLORS["pass"], weight=BOLD)
        shortcut_text.move_to(shortcut.get_center())
        shortcut_arrow = Arrow(shortcut.get_right(), RIGHT * 5.1 + DOWN * 0.45, buff=0.08, color=COLORS["pass"], stroke_width=4)

        out1 = RoundedRectangle(
            corner_radius=0.18,
            width=1.55,
            height=0.8,
            stroke_color=COLORS["accent2"],
            stroke_width=2.2,
            fill_color=COLORS["panel_soft"],
            fill_opacity=1,
        )
        out1.move_to(RIGHT * 3.9 + DOWN * 0.35)
        out1_text = Text("Linear\n258 -> 64", font_size=20, color=COLORS["text"], weight=BOLD)
        out1_text.move_to(out1.get_center())

        out2 = RoundedRectangle(
            corner_radius=0.18,
            width=1.2,
            height=0.8,
            stroke_color=COLORS["accent"],
            stroke_width=2.2,
            fill_color=COLORS["panel_soft"],
            fill_opacity=1,
        )
        out2.move_to(RIGHT * 6.0 + DOWN * 0.35)
        out2_text = Text("Linear\n64 -> 1", font_size=18, color=COLORS["text"], weight=BOLD)
        out2_text.move_to(out2.get_center())

        tanh = RoundedRectangle(
            corner_radius=0.18,
            width=1.1,
            height=0.68,
            stroke_color=COLORS["accent2"],
            stroke_width=2.0,
            fill_color=COLORS["accent2"],
            fill_opacity=0.18,
        )
        tanh.move_to(RIGHT * 7.7 + DOWN * 0.35)
        tanh_text = Text("tanh", font_size=18, color=COLORS["accent2"], weight=BOLD)
        tanh_text.move_to(tanh.get_center())

        formula = MathTex(
            r"y = \tanh\left(W_2\,\mathrm{ReLU}(W_1 h) + w_{pass}\,p\right)",
            color=COLORS["text"],
            font_size=30,
        )
        formula.to_edge(DOWN, buff=0.35)

        self.play(FadeIn(heading, shift=UP * 0.15), FadeIn(sub, shift=UP * 0.15))
        self.play(FadeIn(h_box, shift=LEFT * 0.1), FadeIn(h_text, shift=LEFT * 0.1))
        self.play(LaggedStart(*[FadeIn(p, shift=UP * 0.08) for p in esc_group], lag_ratio=0.12))
        self.play(LaggedStart(*[GrowArrow(a) for a in trunk_arrows], lag_ratio=0.05))
        self.play(LaggedStart(*[FadeIn(block, shift=UP * 0.08) for block in trunk_boxes], lag_ratio=0.12))
        self.play(LaggedStart(*[Create(s) for s in skip_arrows], lag_ratio=0.1))
        self.play(FadeIn(shortcut, shift=UP * 0.1), FadeIn(shortcut_text, shift=UP * 0.1), GrowArrow(shortcut_arrow))
        self.play(FadeIn(out1, shift=RIGHT * 0.12), FadeIn(out1_text, shift=RIGHT * 0.12), FadeIn(out2, shift=RIGHT * 0.12), FadeIn(out2_text, shift=RIGHT * 0.12), FadeIn(tanh, shift=RIGHT * 0.12), FadeIn(tanh_text, shift=RIGHT * 0.12))
        self.play(Write(formula))

        self.play(
            Indicate(shortcut, color=COLORS["pass"]),
            Indicate(tanh, color=COLORS["accent2"]),
            run_time=1.4,
        )

        self.wait(0.8)
        self.play(FadeOut(VGroup(heading, sub, h_box, h_text, esc_group, trunk_boxes, trunk_arrows, skip_arrows, shortcut, shortcut_text, shortcut_arrow, out1, out1_text, out2, out2_text, tanh, tanh_text, formula), shift=DOWN * 0.12))


class BandDPERTrainingDeploy(Scene):
    def construct(self):
        heading = Text("3. Training and deployment", font_size=34, color=COLORS["text"], weight=BOLD)
        heading.to_edge(UP, buff=0.4)
        sub = Text(
            "Minimax bootstrapping labels positions, then the model is exported once and run manually in Java.",
            font_size=20,
            color=COLORS["muted"],
        )
        sub.next_to(heading, DOWN, buff=0.18)

        cards = VGroup()
        card_specs = [
            ("1", "Self-play mix", "Random moves + shallow engine", COLORS["me"]),
            ("2", "Deep labels", "Depth 5-7 minimax targets", COLORS["accent"]),
            ("3", "Train", "AdamW + MSE + cosine restarts", COLORS["accent2"]),
            ("4", "Export", "Weights -> JSON for Java runtime", COLORS["accent3"]),
        ]
        for idx, (num, title, body, color) in enumerate(card_specs):
            card = RoundedRectangle(
                corner_radius=0.18,
                width=2.45,
                height=1.35,
                stroke_color=color,
                stroke_width=2.2,
                fill_color=COLORS["panel"],
                fill_opacity=1,
            )
            card.move_to(np.array([-5.0 + idx * 3.3, 0.1, 0.0]))
            number = Text(num, font_size=20, color=color, weight=BOLD)
            number.move_to(card.get_corner(UL) + np.array([0.28, -0.24, 0]))
            title_text = Text(title, font_size=21, color=COLORS["text"], weight=BOLD)
            title_text.next_to(number, RIGHT, buff=0.16).align_to(number, DOWN)
            body_text = Text(body, font_size=16, color=COLORS["muted"])
            body_text.move_to(card.get_center() + DOWN * 0.22)
            cards.add(VGroup(card, number, title_text, body_text))

        arrows = VGroup()
        for left, right in zip(cards[:-1], cards[1:]):
            arrows.add(Arrow(left[0].get_right(), right[0].get_left(), buff=0.1, color=COLORS["grid"], stroke_width=4))

        metrics = VGroup(
            make_chip("50k-100k positions", COLORS["me"]),
            make_chip("batch size 256", COLORS["accent"]),
            make_chip("drop_last = True", COLORS["accent2"]),
            make_chip("output in [-1, +1]", COLORS["accent3"]),
        ).arrange(RIGHT, buff=0.2)
        metrics.to_edge(DOWN, buff=0.35)

        loop = CurvedArrow(
            cards[0][0].get_bottom() + DOWN * 0.2,
            cards[0][0].get_bottom() + DOWN * 0.2 + RIGHT * 2.1,
            angle=PI / 1.8,
            color=COLORS["me"],
            stroke_width=3,
            tip_length=0.18,
        )
        loop_label = Text("re-bootstrap deeper labels", font_size=16, color=COLORS["me"], weight=BOLD)
        loop_label.next_to(loop, DOWN, buff=0.08)

        self.play(FadeIn(heading, shift=UP * 0.15), FadeIn(sub, shift=UP * 0.15))
        self.play(LaggedStart(*[FadeIn(card, shift=UP * 0.1) for card in cards], lag_ratio=0.15))
        self.play(LaggedStart(*[GrowArrow(a) for a in arrows], lag_ratio=0.08))
        self.play(Create(loop), FadeIn(loop_label, shift=UP * 0.08))
        self.play(LaggedStart(*[FadeIn(metric, shift=DOWN * 0.08) for metric in metrics], lag_ratio=0.12))
        self.play(
            Indicate(metrics[0], color=COLORS["me"]),
            Indicate(metrics[2], color=COLORS["accent2"]),
            Indicate(metrics[3], color=COLORS["accent3"]),
            run_time=1.4,
        )

        footer = Text(
            "BandDPER compresses board structure, tempo, and forced-pass dynamics into one fast scalar evaluator.",
            font_size=22,
            color=COLORS["text"],
            weight=BOLD,
        )
        footer.to_edge(DOWN, buff=0.95)
        self.play(Write(footer))
        self.wait(1.0)


class BandDPERFullVideo(Scene):
    """Master scene that sequences sections with clear transitions and a model overview.

    This scene is designed to run for at least 2 minutes total by spacing sections
    and adding deliberate waits/animations.
    """

    def clear_screen(self, run_time=0.6):
        if len(self.mobjects) > 0:
            # Fade out each top-level mobject individually to avoid mixing Group types
            anims = [FadeOut(m, run_time=run_time) for m in self.mobjects]
            if anims:
                self.play(*anims)

    def construct(self):
        total_time = 0.0

        # --- 1) Explainer snapshot (approx 30s) ---
        title = make_title_block("BandDPER", "Band-aware dual-perspective evaluation network")
        title.to_edge(UP, buff=0.45)
        self.play(Write(title[0]), FadeIn(title[1], shift=UP * 0.12))
        total_time += 1.2

        band_board = make_board(scale=0.88)
        band_board.to_edge(LEFT, buff=0.5)
        self.play(Create(band_board), run_time=1.2)
        total_time += 1.2

        chips = VGroup(
            make_chip("Siamese encoder", COLORS["accent"]),
            make_chip("Residual trunk", COLORS["band2"]),
            make_chip("Forced-pass shortcut", COLORS["pass"]),
        ).arrange(RIGHT, buff=0.25)
        chips.next_to(title, DOWN, buff=0.4)
        self.play(LaggedStart(*[FadeIn(c, shift=UP * 0.12) for c in chips], lag_ratio=0.12))
        total_time += 1.0

        # emphasize board briefly
        self.play(ApplyWave(band_board[0:36], amplitude=0.05, time_width=0.7), run_time=1.8)
        total_time += 1.8

        # hold on explainer content to reach ~30s
        remaining = max(0, 30.0 - total_time)
        self.wait(remaining)
        total_time += remaining

        # clear screen
        self.clear_screen(run_time=0.8)
        total_time += 0.8

        # --- 2) Input encoding snapshot (approx 30s) ---
        heading = Text("Dual-perspective input encoding", font_size=32, color=COLORS["text"], weight=BOLD)
        heading.to_edge(UP, buff=0.4)
        self.play(FadeIn(heading, shift=UP * 0.12))
        total_time += 0.8

        board = make_board(scale=0.72, show_indices=False)
        board.to_edge(LEFT, buff=0.35).shift(DOWN * 0.15)
        self.play(Create(board))
        total_time += 1.2

        # show channel stacks briefly
        x_me = make_channel_stack("x_me")
        x_opp = make_channel_stack("x_opp")
        x_me.to_edge(RIGHT, buff=0.3).shift(UP * 0.65 + LEFT * 0.15)
        x_opp.next_to(x_me, LEFT, buff=0.65)
        self.play(FadeIn(x_opp, shift=RIGHT * 0.1), FadeIn(x_me, shift=RIGHT * 0.1))
        total_time += 1.0

        # emphasize siamese shared weights
        siamese = RoundedRectangle(
            corner_radius=0.18,
            width=2.1,
            height=0.56,
            stroke_color=COLORS["accent2"],
            stroke_width=1.8,
            fill_color=COLORS["accent2"],
            fill_opacity=0.16,
        )
        siamese.move_to(UP * 1.25 + RIGHT * 0.45)
        siamese_text = Text("shared weights", font_size=18, color=COLORS["accent2"], weight=BOLD)
        siamese_text.move_to(siamese.get_center())
        self.play(FadeIn(siamese), FadeIn(siamese_text))
        total_time += 1.0

        # hold to reach ~30s for this segment
        remaining = max(0, 30.0 - total_time)
        if remaining > 0:
            self.wait(remaining)
            total_time += remaining

        self.clear_screen(run_time=0.8)
        total_time += 0.8

        # --- 3) Residual head snapshot (approx 25s) ---
        head_title = Text("Residual trunk & forced-pass head", font_size=30, color=COLORS["text"], weight=BOLD)
        head_title.to_edge(UP, buff=0.4)
        self.play(FadeIn(head_title))
        total_time += 0.6

        # draw trunk blocks
        trunk_blocks = VGroup()
        for idx, color in enumerate([COLORS["accent"], COLORS["band2"], COLORS["accent2"]]):
            block = RoundedRectangle(corner_radius=0.18, width=2.0, height=0.98, stroke_color=color, stroke_width=2.2, fill_color=COLORS["panel"], fill_opacity=1)
            block.move_to(np.array([-1.85 + idx * 2.45, -0.35, 0.0]))
            trunk_blocks.add(block)
        self.play(LaggedStart(*[Create(b) for b in trunk_blocks], lag_ratio=0.12))
        total_time += 1.5

        # show forced-pass shortcut
        shortcut = RoundedRectangle(corner_radius=0.18, width=1.7, height=0.7, stroke_color=COLORS["pass"], stroke_width=2.2, fill_color=COLORS["pass"], fill_opacity=0.16)
        shortcut.move_to(LEFT * 5.05 + UP * 1.45)
        shortcut_text = Text("forced_pass", font_size=19, color=COLORS["pass"], weight=BOLD)
        shortcut_text.move_to(shortcut.get_center())
        self.play(FadeIn(shortcut), FadeIn(shortcut_text))
        total_time += 0.8

        # hold to reach ~25s for this segment
        remaining = max(0, 25.0 - total_time)
        if remaining > 0:
            self.wait(remaining)
            total_time += remaining

        self.clear_screen(run_time=0.8)
        total_time += 0.8

        # --- 4) Training snapshot (approx 20s) ---
        train_title = Text("Training & Export", font_size=30, color=COLORS["text"], weight=BOLD)
        train_title.to_edge(UP, buff=0.4)
        self.play(FadeIn(train_title))
        total_time += 0.6

        # cards
        card_specs = [
            ("1", "Self-play mix", COLORS["me"]),
            ("2", "Deep labels", COLORS["accent"]),
            ("3", "Train", COLORS["accent2"]),
            ("4", "Export", COLORS["accent3"]),
        ]
        cards = VGroup()
        for idx, (num, title, color) in enumerate(card_specs):
            card = RoundedRectangle(corner_radius=0.18, width=2.45, height=1.35, stroke_color=color, stroke_width=2.2, fill_color=COLORS["panel"], fill_opacity=1)
            card.move_to(np.array([-5.0 + idx * 3.3, 0.1, 0.0]))
            number = Text(num, font_size=20, color=color, weight=BOLD)
            number.move_to(card.get_corner(UL) + np.array([0.28, -0.24, 0]))
            title_text = Text(title, font_size=21, color=COLORS["text"], weight=BOLD)
            title_text.next_to(number, RIGHT, buff=0.16).align_to(number, DOWN)
            cards.add(VGroup(card, number, title_text))
        self.play(LaggedStart(*[FadeIn(c, shift=UP * 0.1) for c in cards], lag_ratio=0.12))
        total_time += 1.5

        remaining = max(0, 20.0 - total_time)
        if remaining > 0:
            self.wait(remaining)
            total_time += remaining

        self.clear_screen(run_time=0.8)
        total_time += 0.8

        # --- 5) Model overview (ensure remaining time to reach >= 120s) ---
        overview_title = Text("Model Overview", font_size=36, color=COLORS["text"], weight=BOLD)
        overview_title.to_edge(UP, buff=0.4)
        bullets = VGroup(
            Text("• Siamese spatial encoder (shared weights)", font_size=22, color=COLORS["muted"]),
            Text("• Band-aware residual trunk preserves topology", font_size=22, color=COLORS["muted"]),
            Text("• Forced-pass shortcut adds rule-based bias", font_size=22, color=COLORS["muted"]),
            Text("• Exports to lightweight JSON for Java runtime", font_size=22, color=COLORS["muted"]),
        ).arrange(DOWN, aligned_edge=LEFT, buff=0.26)
        bullets.next_to(overview_title, DOWN, buff=0.8).to_edge(LEFT, buff=1.0)

        self.play(FadeIn(overview_title), LaggedStart(*[FadeIn(b, shift=UP * 0.12) for b in bullets], lag_ratio=0.1))
        total_time += 1.2

        # Add a simple accuracy / perf chart mockup
        chart = RoundedRectangle(corner_radius=0.12, width=5.0, height=2.6, stroke_color=COLORS["grid"], stroke_width=2, fill_color=COLORS["panel"], fill_opacity=1)
        chart.move_to(RIGHT * 2.4 + DOWN * 0.6)
        chart_label = Text("Eval: validation loss / speed", font_size=18, color=COLORS["muted"]) 
        chart_label.move_to(chart.get_top() + DOWN * 0.25)
        self.play(Create(chart), FadeIn(chart_label))
        total_time += 0.8

        # Annotate and hold until total >= 120s
        elapsed = total_time
        remaining = max(0, 120.0 - elapsed)
        # keep some extra seconds for a graceful ending
        if remaining > 0:
            self.wait(remaining)
        self.wait(6.0)

        # final footer and fade out
        footer = Text("BandDPER — compact, fast evaluator for Escampe", font_size=22, color=COLORS["text"], weight=BOLD)
        footer.to_edge(DOWN, buff=0.9)
        self.play(Write(footer))
        self.wait(2.0)
        self.clear_screen(run_time=1.0)

import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';

class DriverMark extends StatelessWidget {
  const DriverMark({super.key});
  @override
  Widget build(BuildContext context) => ExcludeSemantics(
    child: SizedBox.square(
      dimension: 88,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: navy,
          borderRadius: BorderRadius.circular(23),
        ),
        child: CustomPaint(painter: _MarkPainter()),
      ),
    ),
  );
}

class _MarkPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    canvas.scale(size.width / 88, size.height / 88);
    final stem = Path()
      ..moveTo(20, 64)
      ..lineTo(31, 26)
      ..quadraticBezierTo(32, 22, 38, 22)
      ..lineTo(70, 22)
      ..lineTo(65, 33)
      ..lineTo(42, 33)
      ..lineTo(34, 64)
      ..close();
    canvas.drawPath(stem, Paint()..color = Colors.white);
    final bar = Path()
      ..moveTo(40, 40)
      ..lineTo(61, 40)
      ..lineTo(55, 50)
      ..lineTo(37, 50)
      ..close();
    canvas.drawPath(bar, Paint()..color = const Color(0xff679bff));
  }

  @override
  bool shouldRepaint(_MarkPainter oldDelegate) => false;
}

package shipreq.webapp.base.util

import japgolly.scalajs.react.Reusability

abstract class LastValueMemoBoilerplate private[util]() {

  def apply[A, B](f: A => B)(implicit r: Reusability[A]): LastValueMemo[A, B]

  // ===================================================================================================================

  final def apply2[A1, B1, A2, B2, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2])
      (f: (B1, B2) => Z): LastValueMemo[(A1, A2), Z] = {
    type A = (A1, A2)
    implicit val reusability: Reusability[A] =
      Reusability.tuple2(c1.reusability, c2.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2)))
  }

  // ===================================================================================================================

  final def apply3[A1, B1, A2, B2, A3, B3, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3])
      (f: (B1, B2, B3) => Z): LastValueMemo[(A1, A2, A3), Z] = {
    type A = (A1, A2, A3)
    implicit val reusability: Reusability[A] =
      Reusability.tuple3(c1.reusability, c2.reusability, c3.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3)))
  }

  // ===================================================================================================================

  final def apply4[A1, B1, A2, B2, A3, B3, A4, B4, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4])
      (f: (B1, B2, B3, B4) => Z): LastValueMemo[(A1, A2, A3, A4), Z] = {
    type A = (A1, A2, A3, A4)
    implicit val reusability: Reusability[A] =
      Reusability.tuple4(c1.reusability, c2.reusability, c3.reusability, c4.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4)))
  }

  // ===================================================================================================================

  final def apply5[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5])
      (f: (B1, B2, B3, B4, B5) => Z): LastValueMemo[(A1, A2, A3, A4, A5), Z] = {
    type A = (A1, A2, A3, A4, A5)
    implicit val reusability: Reusability[A] =
      Reusability.tuple5(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5)))
  }

  // ===================================================================================================================

  final def apply6[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6])
      (f: (B1, B2, B3, B4, B5, B6) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6), Z] = {
    type A = (A1, A2, A3, A4, A5, A6)
    implicit val reusability: Reusability[A] =
      Reusability.tuple6(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6)))
  }

  // ===================================================================================================================

  final def apply7[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7])
      (f: (B1, B2, B3, B4, B5, B6, B7) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7)
    implicit val reusability: Reusability[A] =
      Reusability.tuple7(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7)))
  }

  // ===================================================================================================================

  final def apply8[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8)
    implicit val reusability: Reusability[A] =
      Reusability.tuple8(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8)))
  }

  // ===================================================================================================================

  final def apply9[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9)
    implicit val reusability: Reusability[A] =
      Reusability.tuple9(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9)))
  }

  // ===================================================================================================================

  final def apply10[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)
    implicit val reusability: Reusability[A] =
      Reusability.tuple10(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10)))
  }

  // ===================================================================================================================

  final def apply11[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)
    implicit val reusability: Reusability[A] =
      Reusability.tuple11(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11)))
  }

  // ===================================================================================================================

  final def apply12[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)
    implicit val reusability: Reusability[A] =
      Reusability.tuple12(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12)))
  }

  // ===================================================================================================================

  final def apply13[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)
    implicit val reusability: Reusability[A] =
      Reusability.tuple13(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13)))
  }

  // ===================================================================================================================

  final def apply14[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)
    implicit val reusability: Reusability[A] =
      Reusability.tuple14(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14)))
  }

  // ===================================================================================================================

  final def apply15[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)
    implicit val reusability: Reusability[A] =
      Reusability.tuple15(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15)))
  }

  // ===================================================================================================================

  final def apply16[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)
    implicit val reusability: Reusability[A] =
      Reusability.tuple16(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16)))
  }

  // ===================================================================================================================

  final def apply17[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)
    implicit val reusability: Reusability[A] =
      Reusability.tuple17(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17)))
  }

  // ===================================================================================================================

  final def apply18[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, A18, B18, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17], c18: LastValueMemo[A18, B18])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)
    implicit val reusability: Reusability[A] =
      Reusability.tuple18(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability, c18.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17), c18(a._18)))
  }

  // ===================================================================================================================

  final def apply19[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, A18, B18, A19, B19, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17], c18: LastValueMemo[A18, B18], c19: LastValueMemo[A19, B19])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)
    implicit val reusability: Reusability[A] =
      Reusability.tuple19(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability, c18.reusability, c19.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17), c18(a._18), c19(a._19)))
  }

  // ===================================================================================================================

  final def apply20[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, A18, B18, A19, B19, A20, B20, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17], c18: LastValueMemo[A18, B18], c19: LastValueMemo[A19, B19], c20: LastValueMemo[A20, B20])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)
    implicit val reusability: Reusability[A] =
      Reusability.tuple20(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability, c18.reusability, c19.reusability, c20.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17), c18(a._18), c19(a._19), c20(a._20)))
  }

  // ===================================================================================================================

  final def apply21[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, A18, B18, A19, B19, A20, B20, A21, B21, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17], c18: LastValueMemo[A18, B18], c19: LastValueMemo[A19, B19], c20: LastValueMemo[A20, B20], c21: LastValueMemo[A21, B21])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, B21) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)
    implicit val reusability: Reusability[A] =
      Reusability.tuple21(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability, c18.reusability, c19.reusability, c20.reusability, c21.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17), c18(a._18), c19(a._19), c20(a._20), c21(a._21)))
  }

  // ===================================================================================================================

  final def apply22[A1, B1, A2, B2, A3, B3, A4, B4, A5, B5, A6, B6, A7, B7, A8, B8, A9, B9, A10, B10, A11, B11, A12, B12, A13, B13, A14, B14, A15, B15, A16, B16, A17, B17, A18, B18, A19, B19, A20, B20, A21, B21, A22, B22, Z]
      (c1: LastValueMemo[A1, B1], c2: LastValueMemo[A2, B2], c3: LastValueMemo[A3, B3], c4: LastValueMemo[A4, B4], c5: LastValueMemo[A5, B5], c6: LastValueMemo[A6, B6], c7: LastValueMemo[A7, B7], c8: LastValueMemo[A8, B8], c9: LastValueMemo[A9, B9], c10: LastValueMemo[A10, B10], c11: LastValueMemo[A11, B11], c12: LastValueMemo[A12, B12], c13: LastValueMemo[A13, B13], c14: LastValueMemo[A14, B14], c15: LastValueMemo[A15, B15], c16: LastValueMemo[A16, B16], c17: LastValueMemo[A17, B17], c18: LastValueMemo[A18, B18], c19: LastValueMemo[A19, B19], c20: LastValueMemo[A20, B20], c21: LastValueMemo[A21, B21], c22: LastValueMemo[A22, B22])
      (f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, B21, B22) => Z): LastValueMemo[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22), Z] = {
    type A = (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)
    implicit val reusability: Reusability[A] =
      Reusability.tuple22(c1.reusability, c2.reusability, c3.reusability, c4.reusability, c5.reusability, c6.reusability, c7.reusability, c8.reusability, c9.reusability, c10.reusability, c11.reusability, c12.reusability, c13.reusability, c14.reusability, c15.reusability, c16.reusability, c17.reusability, c18.reusability, c19.reusability, c20.reusability, c21.reusability, c22.reusability)
    apply[A, Z](a => f(c1(a._1), c2(a._2), c3(a._3), c4(a._4), c5(a._5), c6(a._6), c7(a._7), c8(a._8), c9(a._9), c10(a._10), c11(a._11), c12(a._12), c13(a._13), c14(a._14), c15(a._15), c16(a._16), c17(a._17), c18(a._18), c19(a._19), c20(a._20), c21(a._21), c22(a._22)))
  }
}
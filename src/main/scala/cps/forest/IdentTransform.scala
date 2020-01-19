package cps.forest

import scala.quoted._
import scala.quoted.matching._

import cps._


class IdentTransform[F[_]:Type, T:Type](cpsCtx: TransformationContext[F,T])


  // case Ident(name) 
  def run(given qctx: QuoteContext)(name: String): CpsExpr[F,T] =
     import qctx.tasty.{_, given}
     import cpsCtx._
     CpsExpr.sync(asyncMonad, patternCode) 
  
  


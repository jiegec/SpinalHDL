package spinal.lib.graphic.vga


import spinal.core._
import spinal.lib._
import spinal.lib.bus.avalon.{AvalonMMBus, AvalonMMConfig}
import spinal.lib.bus.neutral.NeutralStreamDma
import spinal.lib.graphic._
import spinal.lib.tool.QSysify

case class AvalonVgaConfig(rgbConfig : RgbConfig){
//  def getControlConfig = AvalonMMConfig.fixed(4,32)
//  def getPictureConfig = AvalonMMConfig.fixed(4,32).getWriteOnlyConfig
}

class AvalonVgaCtrl(cDma : NeutralStreamDma.Config,cColor : RgbConfig) extends Component{
  val io = new Bundle{
//    val control = slave (AvalonMMBus(c.getControlConfig))
    val mem = master (AvalonMMBus(cDma.getAvalonConfig))
    val vga = master(Vga(cColor))
  }

  val dma = new NeutralStreamDma.Block(cDma)


  dma.io.ctrl.cmd.valid := RegNext(True) init(False)
  dma.io.ctrl.cmd.startAt := 0x100000/4
  dma.io.ctrl.cmd.memCmdCount := 640*480/8
  dma.io.ctrl.cmd.burstLength := 8
  dma.io.mem.toAvalon <> io.mem

//  val bufferedDmaMem = dma.io.mem.clone
//  dma.io.mem.cmd.m2sPipe().m2sPipe().m2sPipe().m2sPipe().m2sPipe().m2sPipe() >> bufferedDmaMem.cmd
//  dma.io.mem.rsp << bufferedDmaMem.rsp
//  bufferedDmaMem.toAvalon <> io.mem

  val vga = new ClockingArea(if(cDma.ctrlRspClock != null) cDma.ctrlRspClock else ClockDomain.current) {
    val ctrl = new VgaCtrl(cColor)
    val pixelStream = Stream(Fragment(Rgb(cColor)))
    pixelStream.arbitrationFrom(dma.io.ctrl.rsp)
    pixelStream.last := dma.io.ctrl.rsp.last
    pixelStream.fragment.assignFromBits(dma.io.ctrl.rsp.fragment)

    ctrl.io.timings.setAs_h640_v480_r60
    ctrl.feedWith(pixelStream)
    io.vga := RegNext(ctrl.io.vga)
  }
  //  when(io.control.write){
//    switch(io.control.address){
//      is(0){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(1){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(2){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(3){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(4){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(5){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(6){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//      is(7){
//        ctrl.io.timings.h.syncStart := io.control.writeData.asUInt.resized
//      }
//    }
//  }
}


object AvalonVgaCtrl{
  def main(args: Array[String]) {
    val toplevel = SpinalVhdl({
      val vgaClk = ClockDomain.external("vga")
      val dmaConfig = NeutralStreamDma.Config(
        addressWidth = 30,
        dataWidth = 32,
        memCmdCountMax = 1<<24,
        burstLengthMax = 8,
        fifoSize = 512,
        pendingRequetMax = 4,
        ctrlRspClock = vgaClk
      )
      val colorConfig = RgbConfig(8,8,8)
      new AvalonVgaCtrl(dmaConfig,colorConfig)
    },_.setLibrary("lib_AvalonVgaCtrl")).toplevel

    toplevel.io.mem.addTag(ClockDomainTag(toplevel.clockDomain))
    QSysify(toplevel)

//    SpinalVhdl({
//      val vgaClk = ClockDomain.external("vga")
//      val dmaConfig = NeutralStreamDma.Config(
//        addressWidth = 30,
//        dataWidth = 32,
//        memCmdCountMax = 1<<24,
//        burstLengthMax = 8,
//        fifoSize = 512,
//        pendingRequetMax = 4,
//        ctrlRspClock = vgaClk
//      )
//      val colorConfig = RgbConfig(8,8,8)
//      new AvalonVgaCtrl(dmaConfig,colorConfig).setDefinitionName("TopLevel")
//    })
  }
}


object AvalonVgaCtrlCCTest{
  def main(args: Array[String]) {
    SpinalVhdl({
      val pushClock = ClockDomain.external("pushClock")
      val popClock = ClockDomain.external("popClock")
      new StreamFifoCC(Bits(33 bit),512,pushClock,popClock).setDefinitionName("TopLevel")
    })
  }
}
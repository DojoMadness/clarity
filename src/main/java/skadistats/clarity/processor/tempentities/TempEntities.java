package skadistats.clarity.processor.tempentities;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

@Provides({ OnTempEntity.class })
@UsesDTClasses
public class TempEntities {

    private FieldReader fieldReader;

    @Initializer(OnTempEntity.class)
    public void initOnEntityUpdated(final Context ctx, final EventListener<OnTempEntity> eventListener) {
        fieldReader = ctx.getEngineType().getNewFieldReader();
    }

    @OnMessage(S1NetMessages.CSVCMsg_TempEntities.class)
    public void onTempEntities(Context ctx, S1NetMessages.CSVCMsg_TempEntities message) {
        Event<OnTempEntity> ev = ctx.createEvent(OnTempEntity.class, Entity.class);
        if (ev.isListenedTo()) {
            BitStream stream = new BitStream(message.getEntityData());
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            DTClass cls = null;
            ReceiveProp[] receiveProps = null;
            int count = message.getNumEntries();
            while (count-- > 0) {
                stream.readUBitInt(1); // seems to be always 0
                if (stream.readBitFlag()) {
                    cls = dtClasses.forClassId(stream.readUBitInt(dtClasses.getClassBits()) - 1);
                    receiveProps = cls.getReceiveProps();
                }
                Object[] state = new Object[receiveProps.length];
                fieldReader.readFields(stream, cls, state, false);
                ev.raise(new Entity(0, 0, cls, null, state));
            }
        }
    }

}

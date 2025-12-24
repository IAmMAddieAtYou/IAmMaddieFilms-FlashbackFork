package com.moulberry.flashback.editor.ui.windows;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public record StringValueSet() implements OptionInstance.ValueSet<String> {

    @Override
    public Function<OptionInstance<String>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<String> tooltipSupplier,
            Options options,
            int x, int y, int width,
            Consumer<String> onValueChanged
    ) {
        return (instance) -> {
            EditBox editBox = new EditBox(
                    Minecraft.getInstance().font,
                    x, y, width, 20,
                    instance.caption
            );

            editBox.setValue(instance.get());
            editBox.setMaxLength(1024);

            editBox.setResponder((newValue) -> {
                instance.set(newValue);
                onValueChanged.accept(newValue);
                options.save();
            });

            if (tooltipSupplier != null) {
                editBox.setTooltip(tooltipSupplier.apply(instance.get()));
            }

            return editBox;
        };
    }

    @Override
    public Optional<String> validateValue(String value) {
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public Codec<String> codec() {
        return Codec.STRING;
    }
}

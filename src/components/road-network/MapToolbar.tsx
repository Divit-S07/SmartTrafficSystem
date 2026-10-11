import { Grid3x3, Maximize2, RotateCcw, ZoomIn, ZoomOut } from 'lucide-react';
import { useReactFlow } from 'reactflow';

interface MapToolbarProps {
  showGrid: boolean;
  onToggleGrid: () => void;
  onResetView: () => void;
}

export function MapToolbar({ showGrid, onToggleGrid, onResetView }: MapToolbarProps) {
  const { zoomIn, zoomOut, fitView } = useReactFlow();

  const buttonClass = (active = false) =>
    `w-8 h-8 flex items-center justify-center rounded-lg border shadow-sm transition-colors ${
      active
        ? 'bg-cyan-50 text-cyan-600 border-cyan-300'
        : 'bg-white text-slate-600 border-slate-300 hover:text-cyan-600 hover:border-cyan-400'
    }`;

  return (
    <div className="absolute right-3 top-3 z-20 flex flex-col gap-1.5">
      <button
        type="button"
        className={buttonClass()}
        title="Zoom in"
        aria-label="Zoom in"
        onClick={() => zoomIn({ duration: 200 })}
      >
        <ZoomIn className="h-4 w-4" />
      </button>
      <button
        type="button"
        className={buttonClass()}
        title="Zoom out"
        aria-label="Zoom out"
        onClick={() => zoomOut({ duration: 200 })}
      >
        <ZoomOut className="h-4 w-4" />
      </button>
      <button
        type="button"
        className={buttonClass()}
        title="Fit network to screen"
        aria-label="Fit network to screen"
        onClick={() => fitView({ padding: 0.25, duration: 350 })}
      >
        <Maximize2 className="h-4 w-4" />
      </button>
      <button
        type="button"
        className={buttonClass()}
        title="Reset view"
        aria-label="Reset view"
        onClick={onResetView}
      >
        <RotateCcw className="h-4 w-4" />
      </button>
      <button
        type="button"
        className={buttonClass(showGrid)}
        title={showGrid ? 'Hide alignment grid' : 'Show alignment grid'}
        aria-label={showGrid ? 'Hide alignment grid' : 'Show alignment grid'}
        onClick={onToggleGrid}
      >
        <Grid3x3 className="h-4 w-4" />
      </button>
    </div>
  );
}
